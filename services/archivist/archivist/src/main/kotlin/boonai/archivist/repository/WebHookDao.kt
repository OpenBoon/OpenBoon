package boonai.archivist.repository

import boonai.archivist.domain.InvalidRequestException
import boonai.archivist.domain.TriggerType
import boonai.archivist.domain.WebHook
import boonai.archivist.domain.WebHookFilter
import boonai.archivist.domain.WebHookUpdate
import boonai.common.service.security.getProjectId
import com.google.common.base.Splitter
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
        if (spec.triggers.isEmpty()) {
            throw InvalidRequestException("Your webhook must have triggers")
        }
        val triggers = spec.triggers.map { it.ordinal }.sortedBy { it }.joinToString(",")
        val count = jdbc.update(
            "UPDATE webhook SET url=?, secret_key=?, " +
                "triggers=?, active=? WHERE pk_project=? AND pk_webhook=?",
            spec.url, spec.secretKey, triggers, spec.active, getProjectId(), id
        )
        return count == 1
    }

    companion object {

        const val GET = "SELECT * FROM webhook"
        const val COUNT = "SELECT COUNT(1) FROM webhook"

        private val MAPPER = RowMapper { rs, _ ->
            val triggers = Splitter.on(',').trimResults().omitEmptyStrings().split(rs.getString("triggers"))

            WebHook(
                rs.getObject("pk_webhook") as UUID,
                rs.getObject("pk_project") as UUID,
                rs.getString("url"),
                rs.getString("secret_key"),
                triggers.map { TriggerType.values()[it.toInt()] }.toTypedArray(),
                rs.getBoolean("active"),
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getString("actor_created"),
                rs.getString("actor_modified"),
            )
        }
    }
}
