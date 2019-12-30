package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.domain.DataSourceCredentials
import com.zorroa.archivist.domain.DataSourceFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.StringListConverter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface DataSourceDao : JpaRepository<DataSource, UUID> {

    fun getByName(name: String): DataSource

    fun deleteAllByProjectId(projectUUID: UUID): Int
}

interface DataSourceJdbcDao {

    /**
     * Update a [DataSource] credentials blob. Setting the blob to null
     * will remove it.  The blob must be encrypted before calling this.
     */
    fun updateCredentials(id: UUID, creds: String?, salt: String) : Boolean

    /**
     * Get an encrypted [DataSourceCredentials] blob.
     */
    fun getCredentials(id: UUID) : DataSourceCredentials

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
}


@Repository
class JdbcDataSourceJdbcDaoImpl : AbstractDao(), DataSourceJdbcDao {

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

    override fun updateCredentials(id: UUID, creds: String?, salt: String) : Boolean {
        return jdbc.update("UPDATE datasource SET str_credentials=?, str_salt=? WHERE pk_datasource=? AND pk_project=?",
            creds, salt, id, getProjectId()) == 1
    }

    override fun getCredentials(id: UUID) : DataSourceCredentials {
        return jdbc.queryForObject(
            GET_CREDS,
            RowMapper { rs, _ ->
            DataSourceCredentials(
                rs.getString("str_credentials"),
                rs.getString("str_salt"))
        }, id, getProjectId())
    }

    companion object {

        const val GET = "SELECT * FROM datasource"
        const val COUNT = "SELECT COUNT(1) FROM datasource"

        val converter = StringListConverter()

        private val MAPPER = RowMapper { rs, _ ->
            DataSource(
                rs.getObject("pk_datasource") as UUID,
                rs.getObject("pk_project") as UUID,
                rs.getString("str_name"),
                rs.getString("str_uri"),
                converter.convertToEntityAttribute(rs.getString("str_file_types")),
                converter.convertToEntityAttribute(rs.getString("str_analysis")),
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getString("actor_created"),
                rs.getString("actor_modified")
            )
        }

        const val GET_CREDS = "SELECT " +
                "str_credentials, " +
                "str_salt " +
            "FROM " +
                "datasource " +
            "WHERE " +
                "pk_datasource=? " +
            "AND " +
                "pk_project=? "
    }
}