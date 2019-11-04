package com.zorroa.archivist.clients

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import org.apache.http.client.utils.URIBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * ApiKey instances are what is returned from the auth-server server.
 *
 * @param projectId The project ID of the key.
 * @param keyId  The keyId of the key.
 * @param permissions A list of permissions available to the key.
 * @param indexes The search indexes the key is allowed to use.
 */
data class ApiKey(
    val projectId: UUID,
    val keyId: UUID,
    val permissions: List<String>,
    val indexes : List<String>? = null
) {

    fun getAuthorities(): List<GrantedAuthority> {
        return permissions.map { SimpleGrantedAuthority(it) }
    }
}

/**
 * A simple client to the Authentication service.
 */
class AuthServerClient(baseUri: String) {

    val authServerUri: URI

    val responseType: ParameterizedTypeReference<ApiKey> =
        object : ParameterizedTypeReference<ApiKey>() {}

    val rest: RestTemplate = RestTemplate(HttpComponentsClientHttpRequestFactory())

    init {
        val builder = URIBuilder(baseUri)
        authServerUri = builder.setPath(builder.path + "/auth/v1/auth-token")
            .build()
            .normalize()
    }

    val cache = CacheBuilder.newBuilder()
        .initialCapacity(128)
        .concurrencyLevel(8)
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build(object : CacheLoader<String, ApiKey>() {
            @Throws(Exception::class)
            override fun load(token: String): ApiKey {
                val req = RequestEntity.get(authServerUri)
                    .header("Authorization", "Bearer $token")
                    .accept(MediaType.APPLICATION_JSON).build()
                return rest.exchange(req, responseType).body
            }
        })

    /**
     * Authenticates the given authentication token.
     *
     * @param authToken An authentication token, typically JWT
     */
    fun authenticate(authToken: String): ApiKey {
        return cache.get(authToken)
    }
}