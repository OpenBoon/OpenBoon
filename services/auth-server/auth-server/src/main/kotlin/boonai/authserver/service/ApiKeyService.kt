package boonai.authserver.service

import boonai.authserver.domain.ApiKey
import boonai.authserver.domain.ApiKeyFilter
import boonai.authserver.domain.ApiKeySpec
import boonai.authserver.repository.ApiKeyCustomRepository
import boonai.authserver.repository.ApiKeyRepository
import boonai.authserver.repository.PagedList
import boonai.authserver.security.KeyGenerator
import boonai.authserver.security.getProjectId
import boonai.authserver.security.getZmlpActor
import boonai.common.apikey.Permission
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.service.security.EncryptionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ApiKeyService {

    /**
     * Create a new API key.
     */
    fun create(spec: ApiKeySpec): ApiKey

    fun update(id: UUID, spec: ApiKeySpec): ApiKey

    fun get(id: UUID): ApiKey

    fun findAll(): List<ApiKey>

    fun findOne(filter: ApiKeyFilter): ApiKey

    fun search(filter: ApiKeyFilter): PagedList<ApiKey>

    fun delete(apiKey: ApiKey)

    fun deleteByProject(projectUUID: UUID)

    fun updateEnabled(apiKey: ApiKey, enabled: Boolean)

    fun updateEnabledByProject(projectId: UUID, enabled: Boolean)
}

@Service
@Transactional
class ApiKeyServiceImpl constructor(
    val apikeyCustomRepository: ApiKeyCustomRepository,
    val apiKeyRepository: ApiKeyRepository

) : ApiKeyService {

    @Autowired
    lateinit var encryptionService: EncryptionService

    override fun create(spec: ApiKeySpec): ApiKey {
        if (!getZmlpActor().hasAnyPermission(Permission.SystemServiceKey)) {
            validatePermissionsCanBeAssigned(spec.permissions)
        }

        val time = System.currentTimeMillis()
        val actor = getZmlpActor()
        val key = ApiKey(
            UUID.randomUUID(),
            spec.projectId ?: getProjectId(),
            KeyGenerator.generate(16),
            encryptionService.encryptString(KeyGenerator.generate(16), ApiKey.CRYPT_VARIANCE),
            spec.name,
            spec.permissions.map { it.name }.toSet(),
            time, time,
            actor.toString(),
            actor.toString(),
            spec.enabled,
            spec.name in systemKeys,
            spec.hidden
        )

        logger.event(
            LogObject.API_KEY, LogAction.CREATE,
            mapOf(
                "apiKeyId" to key.id,
                "apiKeyName" to key.name
            )
        )

        try {
            return apiKeyRepository.saveAndFlush(key)
        } catch (ex: DataIntegrityViolationException) {
            throw(DataIntegrityViolationException("Data Integrity Violation: Verify your Api Key"))
        }
    }

    override fun update(id: UUID, spec: ApiKeySpec): ApiKey {

        if (!getZmlpActor().hasAnyPermission(Permission.SystemServiceKey)) {
            validatePermissionsCanBeAssigned(spec.permissions)
        }

        if (spec.name in systemKeys) {
            throw UnsupportedOperationException("This key cannot be changed.")
        }

        val time = System.currentTimeMillis()
        val actor = getZmlpActor()
        val apiKey: ApiKey = get(id)
        val hidden = if (apiKey.systemKey) {
            true
        } else {
            spec.hidden
        }

        val key = ApiKey(
            apiKey.id,
            apiKey.projectId,
            apiKey.accessKey,
            apiKey.secretKey,
            spec.name,
            spec.permissions.map { it.name }.toSet(),
            apiKey.timeCreated, time,
            apiKey.actorCreated,
            actor.toString(),
            spec.enabled,
            apiKey.systemKey,
            hidden
        )

        logger.event(
            LogObject.API_KEY, LogAction.UPDATE,
            mapOf(
                "apiKeyId" to key.id,
                "apiKeyName" to key.name
            )
        )
        apikeyCustomRepository.invalidateCache(apiKey.accessKey)
        return apiKeyRepository.save(key)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): ApiKey {
        return apiKeyRepository.findByProjectIdAndId(getProjectId(), id)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ApiKey> {
        return apiKeyRepository.findAllByProjectIdAndHidden(getProjectId(), false)
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: ApiKeyFilter): ApiKey {
        return apikeyCustomRepository.findOne(filter)
    }

    @Transactional(readOnly = true)
    override fun search(filter: ApiKeyFilter): PagedList<ApiKey> {
        return apikeyCustomRepository.search(filter)
    }

    override fun delete(apiKey: ApiKey) {

        if (apiKey.systemKey) {
            throw UnsupportedOperationException("System Keys Cannot be deleted")
        }

        logger.event(
            LogObject.API_KEY, LogAction.DELETE,
            mapOf(
                "apiKeyId" to apiKey.id,
                "apiKeyName" to apiKey.name
            )
        )
        apiKeyRepository.delete(apiKey)
    }

    override fun deleteByProject(projectUUID: UUID) {
        apiKeyRepository.deleteByProjectId(projectUUID)

        logger.event(
            LogObject.API_KEY, LogAction.DELETE,
            mapOf(
                "projectId" to projectUUID
            )
        )
    }

    override fun updateEnabled(apiKey: ApiKey, enabled: Boolean) {

        logger.event(
            LogObject.API_KEY, if (enabled) LogAction.ENABLE else LogAction.DISABLE,
            mapOf(
                "apiKeyId" to apiKey.id,
                "apiKeyName" to apiKey.name
            )
        )
        apiKeyRepository.updateEnabledById(enabled, apiKey.id)
        apikeyCustomRepository.invalidateCache(apiKey.accessKey)
    }

    override fun updateEnabledByProject(projectId: UUID, enabled: Boolean) {
        val projectId = getProjectId()
        val apiKeyList = apiKeyRepository.findAllByProjectId(projectId)

        apiKeyList.forEach {
            updateEnabled(it, enabled)
        }
    }

    fun validatePermissionsCanBeAssigned(perms: Set<Permission>) {
        perms.forEach {
            if (it.internal) {
                throw IllegalArgumentException("Permission ${it.name} does not exist")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApiKeyServiceImpl::class.java)

        private val systemKeys: Set<String> = setOf("job-runner")
    }
}
