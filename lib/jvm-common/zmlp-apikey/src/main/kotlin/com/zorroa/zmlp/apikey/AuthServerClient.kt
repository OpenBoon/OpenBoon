package com.zorroa.zmlp.apikey

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.zmlp.util.Json.Mapper
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.Date
import java.util.UUID

/**
 * Exceptions thrown from [AuthServerClient]
 */
class AuthServerClientException(message: String) : RuntimeException(message)

interface AuthServerClient {
    fun authenticate(jwtToken: String, projectId: UUID? = null): ZmlpActor
    fun createApiKey(
        project: UUID,
        name: String,
        perms: Collection<Permission>,
        hidden: Boolean = false
    ): ApiKey
    fun getApiKey(projectId: UUID, name: String): ApiKey
    fun getSigningKey(projectId: UUID, name: String): SigningKey
    fun updateApiKeyEnabledByProject(projectId: UUID, enabled: Boolean)
    fun deleteProjectApiKeys(projectId: UUID)

    companion object {

        val PROJECT_ID_HEADER = "X-Zorroa-ProjectId"

        val PROJECT_ID_PARAM = "project_id"
    }
}

/**
 * A simple client to the Authentication service.
 */
open class AuthServerClientImpl(val baseUri: String, private val apiKey: String?) : AuthServerClient {

    val client = OkHttpClient()

    val serviceKey: SigningKey? = loadSigningKey()

    private fun loadSigningKey(): SigningKey? {
        if (apiKey == null) {
            return null
        }
        val path = Paths.get(apiKey)
        return if (Files.exists(path)) {
            val key = Mapper.readValue<SigningKey>(path.toFile())
            logger.debug("Loaded signing key: ${key.accessKey.substring(8)} from: '$apiKey'")
            key
        } else {
            try {
                val decoded = Base64.getUrlDecoder().decode(apiKey)
                val key = Mapper.readValue<SigningKey>(decoded)
                logger.debug("Loaded signing key: ${key.accessKey.substring(8)}")
                key
            } catch (e: Exception) {
                logger.warn("NO signing KEY WAS LOADED", e)
                null
            }
        }
    }

    /**
     * Authenticates the given authentication token.
     *
     * @param jwtToken An JWT token.
     */
    override fun authenticate(jwtToken: String, projectId: UUID?): ZmlpActor {
        val request = Request.Builder()
            .url("$baseUri/auth/v1/auth-token")
            .header("Authorization", "Bearer $jwtToken")
            .also {
                if (projectId != null) {
                    it.header(AuthServerClient.PROJECT_ID_HEADER, it.toString())
                }
            }
            .get().build()

        val rsp = client.newCall(request).execute()
        if (rsp.isSuccessful) {
            val body = rsp.body() ?: throw AuthServerClientException("Invalid APIKey")
            return Mapper.readValue(body.byteStream())
        } else {
            throw AuthServerClientException("Invalid APIKey")
        }
    }

    override fun createApiKey(
        projectId: UUID,
        name: String,
        perms: Collection<Permission>,
        hidden: Boolean
    ): ApiKey {
        val data = mapOf(
            "name" to name,
            "permissions" to perms,
            "hidden" to hidden
        )
        return post("auth/v1/apikey", data, projectId)
    }

    override fun getApiKey(projectId: UUID, name: String): ApiKey {
        val data = mapOf(
            "names" to listOf(name)
        )
        return post("auth/v1/apikey/_findOne", data, projectId)
    }

    override fun updateApiKeyEnabledByProject(projectId: UUID, enabled: Boolean) {

        var enableProjectUrl = "/auth/v1/apikey/_enable_project/$projectId"
        var disableProjectUrl = "/auth/v1/apikey/_disable_project/$projectId"
        val path = if (enabled) enableProjectUrl else disableProjectUrl

        val rbody = RequestBody.create(MEDIA_TYPE_JSON, Mapper.writeValueAsString(emptyMap<String, String>()))
        val req = signRequest(Request.Builder().url("$baseUri/$path".replace("//", "/")), projectId)
            .post(rbody)
            .build()
        val rsp = client.newCall(req).execute()
        if (rsp.code() >= 400) {
            throw AuthServerClientException("AuthServerClient failure, rsp code: ${rsp.code()}")
        }
    }

    override fun getSigningKey(projectId: UUID, name: String): SigningKey {
        return get("auth/v1/apikey/$name/_downloadByName", projectId)
    }

    override fun deleteProjectApiKeys(projectId: UUID) {
        return delete("auth/v1/apikey/_delete_project/$projectId", projectId = projectId)
    }

    private inline fun <reified T> post(path: String, body: Map<String, Any>, projectId: UUID? = null): T {
        val rbody = RequestBody.create(MEDIA_TYPE_JSON, Mapper.writeValueAsString(body))
        val req = signRequest(Request.Builder().url("$baseUri/$path".replace("//", "/")), projectId)
            .post(rbody)
            .build()
        val rsp = client.newCall(req).execute()
        if (rsp.code() >= 400) {
            throw AuthServerClientException("AuthServerClient failure, rsp code: ${rsp.code()}")
        }
        val body = rsp.body() ?: throw AuthServerClientException("AuthServerClient failure, null response body")
        return Mapper.readValue(body.byteStream())
    }

    private inline fun <reified T> delete(path: String, body: Map<String, Any>? = null, projectId: UUID? = null): T {
        val rbody = RequestBody.create(MEDIA_TYPE_JSON, Mapper.writeValueAsString(body))
        val req = signRequest(Request.Builder().url("$baseUri/$path".replace("//", "/")), projectId)
            .delete(rbody)
            .build()
        val rsp = client.newCall(req).execute()
        if (rsp.code() >= 400) {
            throw AuthServerClientException("AuthServerClient failure, rsp code: ${rsp.code()}")
        }
        val body = rsp.body() ?: throw AuthServerClientException("AuthServerClient failure, null response body")
        return Mapper.readValue(body.byteStream())
    }

    private inline fun <reified T> get(path: String, projectId: UUID? = null): T {
        val req = signRequest(Request.Builder().url("$baseUri/$path".replace("//", "/")), projectId)
            .build()
        val rsp = client.newCall(req).execute()
        if (rsp.code() >= 400) {
            throw AuthServerClientException("AuthServerClient failure, rsp code: ${rsp.code()}")
        }
        val body = rsp.body() ?: throw AuthServerClientException("AuthServerClient failure, null response body")
        return Mapper.readValue(body.byteStream())
    }

    /**
     * Signs requests with the servers key.
     */
    fun signRequest(req: Request.Builder, projectId: UUID?): Request.Builder {
        serviceKey?.let {
            val algo = Algorithm.HMAC512(it.secretKey)
            val jwt = JWT.create()
                .withIssuer("zmlp")
                .withExpiresAt(Date(System.currentTimeMillis() + (60 * 1000L)))
                .withClaim("accessKey", it.accessKey)

            projectId?.let { pid ->
                jwt.withClaim("projectId", pid.toString())
            }
            val token = jwt.sign(algo)
            req.header("Authorization", "Bearer $token")
        }
        return req
    }

    companion object {

        val MEDIA_TYPE_JSON: MediaType = MediaType.get("application/json; charset=utf-8")

        private val logger = LoggerFactory.getLogger(AuthServerClient::class.java)
    }
}
