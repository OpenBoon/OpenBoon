package com.zorroa.auth.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

/**
 * Exceptions thrown from [AuthServerClient]
 */
class AuthServerClientException(message: String) : RuntimeException(message)

interface AuthServerClient {
    fun authenticate(jwtToken: String): ZmlpActor
    fun createApiKey(project: UUID, name: String, perms: Collection<Permission>): ApiKey
    fun getApiKey(projectId: UUID, name: String): ApiKey
}
/**
 * A simple client to the Authentication service.
 */
open class AuthServerClientImpl(val baseUri: String, private val apiKey: String?) : AuthServerClient {

    val client = OkHttpClient()

    val serviceKey: SigningiKey? = loadSigningKey()

    private fun loadSigningKey(): SigningiKey? {
        if (apiKey == null) {
            return null
        }
        val path = Paths.get(apiKey)
        return if (Files.exists(path)) {
            val key = Json.mapper.readValue<SigningiKey>(path.toFile())
            logger.debug("Loaded signing key: ${key.keyId.prefix(8)} from: '$apiKey'")
            key
        } else {
            try {
                val decoded = Base64.getUrlDecoder().decode(apiKey)
                val key = Json.mapper.readValue<SigningiKey>(decoded)
                logger.debug("Loaded signing key: ${key.keyId.prefix(8)}")
                key
            } catch (e: Exception) {
                logger.warn("NO signing KEY WAS LOADED")
                null
            }
        }
    }

    /**
     * Authenticates the given authentication token.
     *
     * @param jwtToken An JWT token.
     */
    override fun authenticate(jwtToken: String): ZmlpActor {
        val request = Request.Builder()
            .url("$baseUri/auth/v1/auth-token")
            .header("Authorization", "Bearer $jwtToken")
            .get()
            .build()
        val responseBody = client.newCall(request).execute().body()
            ?: throw AuthServerClientException("Null response from server")
        return Json.mapper.readValue(responseBody.byteStream())
    }

    override fun createApiKey(project: UUID, name: String, perms: Collection<Permission>): ApiKey {
        val data = mapOf(
            "projectId" to project,
            "name" to name,
            "permissions" to perms
        )
        return post("auth/v1/apikey", data)
    }

    override fun getApiKey(projectId: UUID, name: String): ApiKey {
        val data = mapOf(
            "projectIds" to listOf(projectId),
            "names" to listOf(name)
        )
        return post("auth/v1/apikey/_findOne", data)
    }

    private inline fun <reified T> post(path: String, body: Map<String, Any>): T {
        val rbody = RequestBody.create(Json.mediaType, Json.mapper.writeValueAsString(body))
        val req = signRequest(
            Request.Builder()
                .url("$baseUri/$path".replace("//", "/"))
        )
            .post(rbody)
            .build()
        val rsp = client.newCall(req).execute()
        if (rsp.code() >= 400) {
            throw AuthServerClientException("AuthServerClient failure, rsp code: ${rsp.code()}")
        }
        val body = rsp.body() ?: throw AuthServerClientException("AuthServerClient failure, null response body")
        return Json.mapper.readValue(body.byteStream())
    }

    /**
     * Signs requests with the servers key.
     */
    fun signRequest(req: Request.Builder): Request.Builder {
        serviceKey?.let {
            val algo = Algorithm.HMAC512(it.sharedKey)
            val token = JWT.create()
                .withIssuer("auth-server")
                .withClaim("projectId", it.projectId.toString())
                .withClaim("keyId", it.keyId.toString())
                .sign(algo)
            req.header("Authorization", "Bearer $token")
        }
        return req
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthServerClient::class.java)
    }
}
