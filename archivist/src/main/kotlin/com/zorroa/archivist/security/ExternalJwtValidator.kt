package com.zorroa.archivist.security

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.sdk.security.AuthSource
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.sdk.security.UserRegistryService
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * External validators must provide a response in the following format.
 *
 * userId: <unique UUID ID of user>
 * organizationId:  <unique UUID ID org>
 * permissions: comma delimited list of zorroa permission names
 * first_name: the users first name
 * last_name: the users last name
 * mail: the users email
 * locale: the users locale (en_US)
 */
interface ExternalJwtValidator : JwtValidator

class HttpExternalJwtValidator constructor(
    private val validateUri: URI,
    private val userRegistryService: UserRegistryService
) : ExternalJwtValidator {

    var responseType: ParameterizedTypeReference<Map<String, String>> =
        object : ParameterizedTypeReference<Map<String, String>>() {}

    val rest: RestTemplate = RestTemplate(HttpComponentsClientHttpRequestFactory())

    /**
     * Cache the results for a very short amount of time. The token conversion endpoint
     * can be slow depending on how its been implemented.
     */
    private val cache = CacheBuilder.newBuilder()
        .initialCapacity(1024)
        .concurrencyLevel(8)
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .build(object : CacheLoader<String, Map<String, String>>() {
            @Throws(Exception::class)
            override fun load(token: String): Map<String, String> {
                val req = RequestEntity.get(validateUri)
                    .header(JwtSecurityConstants.HEADER_STRING_REQ, JwtSecurityConstants.TOKEN_PREFIX + token)
                    .accept(MediaType.APPLICATION_JSON).build()
                return rest.exchange(req, responseType).body
            }
        })

    override fun validate(token: String): Map<String, String> {
        try {
            return cache.get(token)
        } catch (e: Exception) {
            throw JwtValidatorException("Invalid token")
        }
    }

    override fun provisionUser(claims: Map<String, String>): UserAuthed? {

        val userId = claims.getValue("userId")
        val username = claims.getOrDefault("username", userId)
        val organizationId = claims.getValue("organizationId")

        val source = AuthSource(
            "Jwt", AUTH_SOURCE, "Jwt", organizationId,
            attrs = claims,
            groups = claims["permissions"]?.split(",")?.map { it.trim() },
            userId = UUID.fromString(userId)
        )
        logger.info("Jwt provision user: $userId/$username in OrgId $organizationId")
        return userRegistryService.registerUser(username, source)
    }

    companion object {

        /**
         * The name of the auth-source cannot be changed.
         ***/
        const val AUTH_SOURCE = "Jwt"

        private val logger = LoggerFactory.getLogger(ExternalJwtValidator::class.java)
    }
}
