package boonai.archivist.repository

import boonai.archivist.domain.Credentials
import boonai.archivist.domain.DataSource
import boonai.archivist.domain.DataSourceFilter
import boonai.archivist.domain.FileType
import boonai.common.service.jpa.StringListConverter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.AttributeConverter

interface DataSourceDao : JpaRepository<DataSource, UUID> {
    fun getOneByProjectIdAndName(projectId: UUID, name: String): DataSource
    fun getOneByProjectIdAndId(projectId: UUID, id: UUID): DataSource
}

class FileTypeConverter : AttributeConverter<List<FileType>, String> {

    override fun convertToDatabaseColumn(list: List<FileType>?): String? {
        return (list ?: FileType.allTypes()).sorted().joinToString(",") { it.name }
    }

    override fun convertToEntityAttribute(joined: String): List<FileType> {
        return FileType.fromString(joined)
    }
}

interface DataSourceJdbcDao {

    /**
     * Find a [KPagedList] of [DataSources] that match the given [DataSourceFilter]
     * The [Project] filter is applied automatically.
     */
    fun find(filter: DataSourceFilter): KPagedList<DataSource>

    /**
     * Find one and only one [KPagedList] of [DataSources] that match
     * the given [DataSourceFilter]. The [Project] filter is applied automatically.
     */
    fun findOne(filter: DataSourceFilter): DataSource

    /**
     * Count the number of [DataSources] that match the given [DataSourceFilter]
     * The [Project] filter is applied automatically.
     */
    fun count(filter: DataSourceFilter): Long

    /**
     * Set the default credentials for this DataSource
     */
    fun setCredentials(id: UUID, creds: List<Credentials>)
}

@Repository
class DataSourceJdbcDaoImpl : AbstractDao(), DataSourceJdbcDao {

    override fun setCredentials(id: UUID, creds: List<Credentials>) {
        jdbc.update("DELETE FROM x_credentials_datasource WHERE pk_datasource=?", id)
        creds.forEach {
            jdbc.update(
                "INSERT INTO x_credentials_datasource VALUES (?,?,?,?)",
                UUID.randomUUID(), it.id, id, it.type.ordinal
            )
        }
    }

    override fun count(filter: DataSourceFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun findOne(filter: DataSourceFilter): DataSource {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("DataSource not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun find(filter: DataSourceFilter): KPagedList<DataSource> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    private val MAPPER = RowMapper { rs, _ ->
        DataSource(
            rs.getObject("pk_datasource") as UUID,
            rs.getObject("pk_project") as UUID,
            rs.getString("str_name"),
            rs.getString("str_uri"),
            FileType.fromString(rs.getString("str_file_types")),
            jdbc.queryForList(
                "SELECT pk_credentials FROM x_credentials_datasource WHERE pk_datasource=?",
                UUID::class.java, rs.getObject("pk_datasource")
            ),
            jdbc.queryForList(
                "SELECT pk_module FROM x_module_datasource WHERE pk_datasource=?",
                UUID::class.java, rs.getObject("pk_datasource")
            ),
            rs.getLong("time_created"),
            rs.getLong("time_modified"),
            rs.getString("actor_created"),
            rs.getString("actor_modified")
        )
    }

    companion object {

        const val GET = "SELECT * FROM datasource"
        const val COUNT = "SELECT COUNT(1) FROM datasource"

        private val converter = StringListConverter()
    }
}
