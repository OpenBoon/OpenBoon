package boonai.archivist.repository

import boonai.archivist.domain.TriggerType
import boonai.archivist.domain.WebHook
import boonai.archivist.domain.WebHookFilter
import boonai.archivist.domain.WebHookUpdate
import boonai.common.service.security.getProjectId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WebHookDao : JpaRepository<WebHook, UUID> {

    fun getAllByProjectId(projectId: UUID): List<WebHook>
    fun getAllByActiveAndProjectId(active: Boolean, projectId: UUID): List<WebHook>
}

@Repository
interface CustomWebHookDao {
    fun count(filter: WebHookFilter): Long
    fun findOne(filter: WebHookFilter): WebHook
    fun getAll(filter: WebHookFilter): KPagedList<WebHook>
    fun update(id: UUID, spec: WebHookUpdate): Boolean
}

@Repository
class CustomWebHookDaoImpl : CustomWebHookDao, AbstractDao() {

    override fun count(filter: WebHookFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun findOne(filter: WebHookFilter): WebHook {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("Webhook not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun getAll(filter: WebHookFilter): KPagedList<WebHook> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun update(id: UUID, spec: WebHookUpdate): Boolean {
        val triggers = spec.triggers.map { it.ordinal }.joinToString(",")
        return jdbc.update(
            "UPDATE webhook SET url=?, secret_token=?, " +
                "triggers=?, active=? WHERE pk_project=? AND pk_webhook=?",
            spec.url, spec.secretToken, triggers, spec.active, getProjectId(), id
        ) == 1
    }

    companion object {

        const val GET = "SELECT * FROM webhook"
        const val COUNT = "SELECT COUNT(1) FROM webhook"

        private val MAPPER = RowMapper { rs, _ ->
            WebHook(
                rs.getObject("pk_field") as UUID,
                rs.getObject("pk_project") as UUID,
                rs.getString("url"),
                rs.getString("secret_token"),
                rs.getString("triggers").split(",").map { TriggerType.values()[it.toInt()] }.toTypedArray(),
                rs.getBoolean("active"),
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getString("actor_created"),
                rs.getString("actor_modified"),
            )
        }
    }
}
