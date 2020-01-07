package com.zorroa.auth.server.service

import com.zorroa.auth.client.Permission
import com.zorroa.auth.server.domain.ApiKey
import com.zorroa.auth.server.domain.ApiKeyFilter
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.auth.server.repository.ApiKeyRepository
import com.zorroa.auth.server.repository.ApiKeySearchRepository
import com.zorroa.auth.server.security.KeyGenerator
import com.zorroa.auth.server.security.getProjectId
import com.zorroa.auth.server.security.getZmlpActor
import java.util.UUID
import org.springframework.stereotype.Service

interface ApiKeyService {

    /**
     * Create a new API key.
     */
    fun create(spec: ApiKeySpec): ApiKey

    fun get(id: UUID): ApiKey

    fun findAll(): List<ApiKey>

    fun findOne(filter: ApiKeyFilter): ApiKey

    fun delete(apiKey: ApiKey)
}

@Service
class ApiKeyServiceImpl constructor(
    val apikeySearchRepository: ApiKeySearchRepository,
    val apiKeyRepository: ApiKeyRepository

) : ApiKeyService {

    override fun create(spec: ApiKeySpec): ApiKey {
        val actor = getZmlpActor()

        // If the Actor is a PlatformApp, then allow a project ID override.
        val projectId = if (actor.hasAnyPermission(Permission.SystemProjectOverride) && spec.projectId != null) {
            spec.projectId
        } else {
            actor.projectId
        }

        val key = ApiKey(
            UUID.randomUUID(),
            projectId,
            KeyGenerator.generate(24),
            KeyGenerator.generate(64),
            spec.name,
            spec.permissions.map { it.name }.toSet()
        )
        return apiKeyRepository.save(key)
    }

    override fun get(id: UUID): ApiKey {
        return apiKeyRepository.findByProjectIdAndId(getProjectId(), id)
    }

    override fun findAll(): List<ApiKey> {
        return apiKeyRepository.findAllByProjectId(getProjectId())
    }

    override fun findOne(filter: ApiKeyFilter): ApiKey {
        return apikeySearchRepository.findOne(filter)
    }

    override fun delete(apiKey: ApiKey) {
        apiKeyRepository.delete(apiKey)
    }
}
