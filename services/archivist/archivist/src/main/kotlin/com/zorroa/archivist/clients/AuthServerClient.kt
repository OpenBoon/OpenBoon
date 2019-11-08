package com.zorroa.archivist.clients

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
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
 */
data class ZmlpUser(
    val keyId: UUID,
    val projectId: UUID,
    val name: String,
    val permissions: List<String>
) {

    fun getAuthorities(): List<GrantedAuthority> {
        return permissions.map { SimpleGrantedAuthority(it) }
    }
}

interface AuthServerClient {

    val rest: RestTemplate

    fun authenticate(authToken: String): ZmlpUser
}

/**
 * A simple client to the Authentication service.
 */
class AuthServerClientImpl(val baseUri: String) : AuthServerClient {

    val responseType: ParameterizedTypeReference<ZmlpUser> =
        object : ParameterizedTypeReference<ZmlpUser>() {}

    override val rest: RestTemplate = RestTemplate(HttpComponentsClientHttpRequestFactory())

    val cache = CacheBuilder.newBuilder()
        .initialCapacity(128)
        .concurrencyLevel(8)
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build(object : CacheLoader<String, ZmlpUser>() {
            @Throws(Exception::class)
            override fun load(token: String): ZmlpUser {
                val req = RequestEntity.get(URI("${baseUri}/auth/v1/auth-token"))
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
    override fun authenticate(authToken: String): ZmlpUser {
        return cache.get(authToken)
    }
}