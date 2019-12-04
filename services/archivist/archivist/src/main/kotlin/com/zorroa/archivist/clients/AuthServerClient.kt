package com.zorroa.archivist.clients

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.util.Json
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * ZmlpUser instances are the result of authenticating a JWT token
 * with the auth server.
 *
 * @param keyId  The keyId of the key.
 * @param projectId The project ID of the key.
 * @param name a name assoicated with they key, names are unique.
 * @param permissions A list of permissions available to the key.
 */
class ZmlpActor(
    val keyId: UUID,
    val projectId: UUID,
    val name: String,
    val permissions: List<String>
) {

    /**
     * Convet the permissions list to an array of GrantedAuthority.
     */
    fun getAuthorities(): List<GrantedAuthority> {
        return permissions.map { SimpleGrantedAuthority(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ZmlpActor

        if (keyId != other.keyId) return false

        return true
    }

    override fun hashCode(): Int {
        return keyId.hashCode()
    }
}

/**
 * An ApiKey that can be used for signing JWT tokens.
 */
class ApiKey(
    val keyId: UUID,
    val projectId: UUID,
    val sharedKey: String
) {

    fun toBase64(): String {
        return Base64.getEncoder().encodeToString(Json.serialize(this))
    }
}

interface AuthServerClient {

    val rest: RestTemplate

    /**
     * Authenticate the given JWT token with the auth-server and return
     * a ZMLP user instance.
     */
    fun authenticate(authToken: String): ZmlpActor

    /**
     * Create a new API key for the given project.
     */
    fun createApiKey(project: Project, name: String, perms: List<String>): ApiKey

    /**
     * Get an API key by project and its unique name.
     */
    fun getApiKey(projectId: UUID, name: String): ApiKey
}

/**
 * A simple client to the Authentication service.
 */
class AuthServerClientImpl(val baseUri: String) : AuthServerClient {

    override val rest: RestTemplate = RestTemplate(HttpComponentsClientHttpRequestFactory())

    val cache = CacheBuilder.newBuilder()
        .initialCapacity(128)
        .concurrencyLevel(8)
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build(object : CacheLoader<String, ZmlpActor>() {
            @Throws(Exception::class)
            override fun load(token: String): ZmlpActor {
                val req = RequestEntity.get(URI("${baseUri}/auth/v1/auth-token"))
                    .header("Authorization", "Bearer $token")
                    .accept(MediaType.APPLICATION_JSON).build()
                return rest.exchange(req, TYPE_ZMLPUSER).body
            }
        })

    private val serviceKey: ApiKey? = detectServiceKey()

    private fun detectServiceKey(): ApiKey? {
        val cfgPath = System.getenv().getOrDefault("ARCHIVIST_CONFIG_PATH", "/config")
        val keyPath = Paths.get("$cfgPath/inception-key.json")
        return if (Files.exists(keyPath)) {
            Json.Mapper.readValue(keyPath.toFile())
        } else {
            null
        }
    }

    /**
     *
     * Authenticates the given authentication token.
     *
     * @param authToken An authentication token, typically JWT
     */
    override fun authenticate(authToken: String): ZmlpActor {
        return cache.get(authToken)
    }

    override fun createApiKey(project: Project, name: String, perms: List<String>): ApiKey {
        val body = mapOf(
            "projectId" to project.id,
            "name" to name,
            "permissions" to perms
        )
        val req = signRequest(RequestEntity.post(URI("${baseUri}/auth/v1/apikey")))
            .body(body)
        return rest.exchange(req, TYPE_APIKEY).body
    }

    override fun getApiKey(projectId: UUID, name: String): ApiKey {
        val body = mapOf(
            "projectIds" to listOf(projectId),
            "names" to listOf(name)
        )
        val req = signRequest(
            RequestEntity.post(URI("${baseUri}/auth/v1/apikey/_findOne"))
        ).body(body)
        return rest.exchange(req, TYPE_APIKEY).body
    }

    /**
     * Signs requests with the super key.
     */
    fun signRequest(entity: RequestEntity.BodyBuilder): RequestEntity.BodyBuilder {
        serviceKey?.let {
            val algo = Algorithm.HMAC512(it.sharedKey)
            val token = JWT.create()
                .withIssuer("zorroa-archivist")
                .withClaim("projectId", it.projectId.toString())
                .withClaim("keyId", it.keyId.toString())
                .sign(algo)
            entity.header("Authorization", "Bearer $token")
        }
        return entity
    }

    companion object {

        // A couple convinience ParameterizedTypeReferences for
        // calling RestTemplate.exchange.

        val TYPE_ZMLPUSER: ParameterizedTypeReference<ZmlpActor> =
            object : ParameterizedTypeReference<ZmlpActor>() {}

        val TYPE_APIKEY: ParameterizedTypeReference<ApiKey> =
            object : ParameterizedTypeReference<ApiKey>() {}
    }
}