package com.zorroa.auth.service

import com.google.common.hash.Hashing
import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.ApiKeySpec
import com.zorroa.auth.repository.ApiKeyRepository
import com.zorroa.auth.security.getProjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

interface ApiKeyService {

    /**
     * Create a new API key.
     */
    fun create(spec: ApiKeySpec): ApiKey

    fun get(id: UUID) : ApiKey

    fun findAll() : List<ApiKey>

    fun delete(apiKey: ApiKey)
}

@Service
class ApiKeyServiceImpl : ApiKeyService {

    @Autowired
    lateinit var apiKeyRepository: ApiKeyRepository

    override fun create(spec: ApiKeySpec) : ApiKey {

        val authorities = when {
            spec.permissions.isNullOrEmpty() -> "READ"
            else -> spec.permissions.joinToString(",")
        }

        val key = ApiKey(
                UUID.randomUUID(),
                spec.projectId,
                KeyGenerator.generate(),
                spec.name,
                authorities
        )
        return apiKeyRepository.save(key)
    }

    override fun get(keyId: UUID) : ApiKey {
       return apiKeyRepository.findByKeyIdAndProjectId(keyId, getProjectId())
    }

    override fun findAll() : List<ApiKey> {
        return apiKeyRepository.findAllByProjectId(getProjectId())
    }

    override fun delete(apiKey: ApiKey) {
        apiKeyRepository.delete(apiKey)
    }
}


object KeyGenerator {

    private val hashFunc = Hashing.sha512()

    fun generate(): String {
        return hashFunc.newHasher()
                .putString(UUID.randomUUID().toString(), Charsets.UTF_8)
                .putString(UUID.randomUUID().toString(), Charsets.UTF_8)
                .hash().toString()
    }
}
