package com.zorroa.auth.service

import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.ApiKeyFilter
import com.zorroa.auth.domain.ApiKeySpec
import com.zorroa.auth.domain.KeyGenerator
import com.zorroa.auth.repository.ApiKeyRepository
import com.zorroa.auth.repository.ApiKeySearchRepository
import com.zorroa.auth.security.getProjectId
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
        val key = ApiKey(
            UUID.randomUUID(),
            spec.projectId,
            KeyGenerator.generate(),
            spec.name,
            spec.permissions
        )
        return apiKeyRepository.save(key)
    }

    override fun get(id: UUID): ApiKey {
        return apiKeyRepository.findByProjectIdAndid(getProjectId(), id)
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
