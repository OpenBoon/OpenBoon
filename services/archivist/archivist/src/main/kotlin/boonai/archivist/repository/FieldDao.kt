package boonai.archivist.repository

import boonai.archivist.domain.Field
import boonai.archivist.domain.FieldFilter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FieldDao : JpaRepository<Field, UUID> {

    fun getAllByProjectId(projectId: UUID): List<Field>
    fun getByProjectIdAndId(projectId: UUID, id: UUID): Field
    fun getByProjectIdAndName(projectId: UUID, name: String): Field
}

@Repository
interface CustomFieldDao {
    fun count(filter: FieldFilter): Long
    fun findOne(filter: FieldFilter): Field
    fun getAll(filter: FieldFilter): KPagedList<Field>
}

@Repository
class CustomFieldDaoImpl : CustomFieldDao, AbstractDao() {

    override fun count(filter: FieldFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun findOne(filter: FieldFilter): Field {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("Field not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun getAll(filter: FieldFilter): KPagedList<Field> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    companion object {

        const val GET = "SELECT * FROM field"
        const val COUNT = "SELECT COUNT(1) FROM field"

        private val MAPPER = RowMapper { rs, _ ->
            Field(
                rs.getObject("pk_field") as UUID,
                rs.getObject("pk_project") as UUID,
                rs.getString("str_name"),
                rs.getString("str_type"),
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getString("actor_created"),
                rs.getString("actor_modified"),
            )
        }
    }
}
