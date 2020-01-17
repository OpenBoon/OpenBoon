package com.zorroa.auth.server.service

import com.zorroa.auth.server.domain.ApiKey
import com.zorroa.auth.server.domain.ApiKeyFilter
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.auth.server.repository.ApiKeyCustomRepository
import com.zorroa.auth.server.repository.ApiKeyRepository
import com.zorroa.auth.server.repository.PagedList
import com.zorroa.auth.server.security.KeyGenerator
import com.zorroa.auth.server.security.getProjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ApiKeyService {

    /**
     * Create a new API key.
     */
    fun create(spec: ApiKeySpec): ApiKey

    fun get(id: UUID): ApiKey

    fun findAll(): List<ApiKey>

    fun findOne(filter: ApiKeyFilter): ApiKey

    fun search(filter: ApiKeyFilter): PagedList<ApiKey>

    fun delete(apiKey: ApiKey)
}

@Service
@Transactional
class ApiKeyServiceImpl constructor(
    val apikeyCustomRepository: ApiKeyCustomRepository,
    val apiKeyRepository: ApiKeyRepository

) : ApiKeyService {

    override fun create(spec: ApiKeySpec): ApiKey {
        val key = ApiKey(
            UUID.randomUUID(),
            getProjectId(),
            KeyGenerator.generate(24),
            KeyGenerator.generate(64),
            spec.name,
            spec.permissions.map { it.name }.toSet()
        )
        return apiKeyRepository.save(key)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): ApiKey {
        return apiKeyRepository.findByProjectIdAndId(getProjectId(), id)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ApiKey> {
        return apiKeyRepository.findAllByProjectId(getProjectId())
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
        apiKeyRepository.delete(apiKey)
    }
}
