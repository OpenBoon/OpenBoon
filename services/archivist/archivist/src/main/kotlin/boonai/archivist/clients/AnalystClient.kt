package boonai.archivist.clients

import boonai.archivist.domain.TaskState
import boonai.common.apikey.AuthServerClientException
import boonai.common.util.Json
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class AnalystClient(val baseUri: String) {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5000, TimeUnit.MILLISECONDS)
        .readTimeout(5000, TimeUnit.MILLISECONDS)
        .callTimeout(5000, TimeUnit.MILLISECONDS)
        .build()

    private inline fun <reified T> post(path: String, body: Map<String, Any>): T {
        val rbody = RequestBody.create(MEDIA_TYPE_JSON, Json.Mapper.writeValueAsString(body))
        val req = Request.Builder().url("$baseUri/$path".replace("//", "/"))
            .post(rbody)
            .build()

        client.newCall(req).execute().use { rsp ->
            if (rsp.code() >= 400) {
                throw AuthServerClientException("AuthServerClient failure, rsp code: ${rsp.code()}")
            }
            val body = rsp.body() ?: throw AuthServerClientException("AuthServerClient failure, null response body")
            return Json.Mapper.readValue(body.byteStream())
        }
    }

    private inline fun <reified T> delete(path: String, body: Map<String, Any>? = null): T {
        val rbody = RequestBody.create(MEDIA_TYPE_JSON, Json.Mapper.writeValueAsString(body))
        val req = Request.Builder().url("$baseUri/$path".replace("//", "/"))
            .delete(rbody)
            .build()

        client.newCall(req).execute().use { rsp ->
            if (rsp.code() >= 400) {
                throw AuthServerClientException("AuthServerClient failure, rsp code: ${rsp.code()}")
            }
            val body = rsp.body() ?: throw AuthServerClientException("AuthServerClient failure, null response body")
            return Json.Mapper.readValue(body.byteStream())
        }
    }

    private inline fun <reified T> get(path: String): T {
        val req = Request.Builder().url("$baseUri/$path".replace("//", "/")).build()

        client.newCall(req).execute().use { rsp ->
            if (rsp.code() >= 400) {
                throw AuthServerClientException("AuthServerClient failure, rsp code: ${rsp.code()}")
            }
            val body = rsp.body() ?: throw AuthServerClientException("AuthServerClient failure, null response body")
            return Json.Mapper.readValue(body.byteStream())
        }
    }

    fun killTask(taskId: UUID, reason: String, newState: TaskState): Map<String, Any> {
        return delete(
            "/kill/$taskId",
            mapOf("reason" to reason, "newState" to newState.name)
        )
    }

    companion object {

        val MEDIA_TYPE_JSON: MediaType = MediaType.get("application/json; charset=utf-8")

        private val logger = LoggerFactory.getLogger(AnalystClient::class.java)
    }
}
