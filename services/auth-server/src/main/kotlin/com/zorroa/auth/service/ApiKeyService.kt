package com.zorroa.auth.service

import com.google.common.hash.Hashing
import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.ApiKeyFilter
import com.zorroa.auth.domain.ApiKeySpec
import com.zorroa.auth.repository.ApiKeyRepository
import com.zorroa.auth.repository.ApiKeySearchRepository
import com.zorroa.auth.security.getProjectId
import org.springframework.stereotype.Service
import java.util.UUID

interface ApiKeyService {

    /**
     * Create a new API key.
     */
    fun create(spec: ApiKeySpec): ApiKey

    fun get(keyId: UUID): ApiKey

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

    override fun get(keyId: UUID): ApiKey {
        return apiKeyRepository.findByProjectIdAndKeyId(getProjectId(), keyId)
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

object KeyGenerator {

    private val hashFunc = Hashing.sha256()

    fun generate(): String {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        return hashFunc.newHasher()
            .putLong(id1.mostSignificantBits)
            .putLong(id1.leastSignificantBits)
            .putLong(id2.mostSignificantBits)
            .putLong(id2.leastSignificantBits)
            .hash().toString()
    }
}
