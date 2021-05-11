package boonai.archivist.service

import boonai.archivist.domain.InvalidRequestException
import boonai.archivist.domain.WebHook
import boonai.archivist.domain.WebHookFilter
import boonai.archivist.domain.WebHookSpec
import boonai.archivist.domain.WebHookUpdate
import boonai.archivist.repository.CustomWebHookDao
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.WebHookDao
import boonai.archivist.security.getProjectId
import boonai.archivist.util.validateUrl
import boonai.common.service.security.getZmlpActor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface WebHookService {
    fun createWebHook(spec: WebHookSpec): WebHook
    fun getActiveWebHooks(): List<WebHook>
    fun getActiveWebHooks(projectId: UUID): List<WebHook>
    fun deleteWebHook(hook: WebHook)
    fun getWebHook(id: UUID): WebHook
    fun update(id: UUID, hook: WebHookUpdate): Boolean
    fun findWebHooks(filter: WebHookFilter): KPagedList<WebHook>
    fun findOneWebHook(filter: WebHookFilter): WebHook
    fun validateUrl(url: String)
}

@Service
@Transactional
class WebHookServiceImpl constructor(
    private val webHookDao: WebHookDao,
    private val customWebHookDao: CustomWebHookDao
) : WebHookService {

    override fun createWebHook(spec: WebHookSpec): WebHook {
        val id = UUID.randomUUID()
        val time = System.currentTimeMillis()
        val actor = getZmlpActor().toString()
        spec.triggers.sortBy { it.ordinal }

        if (spec.triggers.isEmpty()) {
            throw InvalidRequestException("You must have at least 1 trigger.")
        }

        this.validateUrl(spec.url)

        val hook = WebHook(
            id,
            getProjectId(),
            spec.url,
            spec.secretKey,
            spec.triggers,
            spec.active,
            time,
            time,
            actor,
            actor
        )

        webHookDao.saveAndFlush(hook)
        return hook
    }

    @Transactional(readOnly = true)
    override fun getActiveWebHooks(): List<WebHook> {
        return webHookDao.getAllByActiveAndProjectId(true, getProjectId())
    }

    @Transactional(readOnly = true)
    override fun getActiveWebHooks(projectId: UUID): List<WebHook> {
        return webHookDao.getAllByActiveAndProjectId(true, projectId)
    }

    @Transactional(readOnly = true)
    override fun getWebHook(id: UUID): WebHook {
        return webHookDao.getOne(id)
    }

    override fun deleteWebHook(hook: WebHook) {
        webHookDao.delete(hook)
    }

    @Transactional(readOnly = true)
    override fun findOneWebHook(filter: WebHookFilter): WebHook {
        return customWebHookDao.findOne(filter)
    }

    @Transactional(readOnly = true)
    override fun findWebHooks(filter: WebHookFilter): KPagedList<WebHook> {
        return customWebHookDao.getAll(filter)
    }

    override fun update(id: UUID, hook: WebHookUpdate): Boolean {
        return customWebHookDao.update(id, hook)
    }

    /**
     * Make sure we're not using any invalid IP addresses.
     */
    override fun validateUrl(url: String) {
        val testMode = System.getenv("PUBSUB_EMULATOR_HOST") != null
        return validateUrl(url, testMode)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebHookServiceImpl::class.java)
    }
}
