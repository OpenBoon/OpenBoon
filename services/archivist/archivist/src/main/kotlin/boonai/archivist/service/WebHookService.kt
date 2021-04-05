package boonai.archivist.service

import boonai.archivist.domain.WebHook
import boonai.archivist.domain.WebHookFilter
import boonai.archivist.domain.WebHookSpec
import boonai.archivist.domain.WebHookUpdate
import boonai.archivist.repository.CustomWebHookDao
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.WebHookDao
import boonai.archivist.security.getProjectId
import boonai.common.service.security.getZmlpActor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface WebHookService {
    fun createWebHook(spec: WebHookSpec): WebHook
    fun getActiveWebHooks(): List<WebHook>
    fun deleteWebHook(hook: WebHook)
    fun getWebHook(id: UUID): WebHook
    fun update(id: UUID, hook: WebHookUpdate): Boolean
    fun findWebHooks(filter: WebHookFilter): KPagedList<WebHook>
    fun findOneWebHook(filter: WebHookFilter): WebHook
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

        val hook = WebHook(
            id,
            getProjectId(),
            spec.url,
            spec.secretToken,
            spec.triggers,
            true,
            time,
            time,
            actor,
            actor
        )

        webHookDao.save(hook)
        return hook
    }

    @Transactional(readOnly = true)
    override fun getActiveWebHooks(): List<WebHook> {
        return webHookDao.getAllByActiveAndProjectId(true, getProjectId())
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
}
