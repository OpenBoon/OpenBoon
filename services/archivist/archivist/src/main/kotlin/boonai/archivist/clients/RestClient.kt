package boonai.archivist.clients

import com.fasterxml.jackson.core.type.TypeReference
import boonai.common.util.Json
import org.apache.commons.codec.binary.Hex
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class RestClientException constructor(
    override val message:
        String,
    override val cause: Throwable?
) : RuntimeException(message, cause) {

    var status: Int = HttpStatus.SC_INTERNAL_SERVER_ERROR

    constructor(message: String) : this(message, null)
    constructor(message: String, status: Int) : this(message, null) {
        this.status = status
    }
}

/**
 * A Hmac client for the archivist.
 *
 * For auto-configuration, some environment variables can be set.
 * ZORROA_ARCHIVIST_URL - the base URL for the archivist.
 * ZORROA_HMAC_PATH - the location the HMAC keys.
 * ZORROA_HMAC_KEY - the user's hmac key, overrides ZORROA_HMAC_PATH
 * ZORROA_USER - the username making the request.
 */
class RestClient {

    private var user: String? = null
    private var hmac: String? = null
    private val host: HttpHost
    private val client: CloseableHttpClient
    private var retryConnectionTimeout = false

    constructor() {
        this.host = initHost()
        this.user = initUser()
        this.hmac = initHmacKey()
        this.client = initClient()
    }

    constructor(host: String) {
        this.host = HttpHost.create(host)
        this.client = initClient()
        this.user = initUser()
    }

    fun isRetryConnectionTimeout(): Boolean {
        return retryConnectionTimeout
    }

    fun setRetryConnectionTimeout(retryConnectionTimeout: Boolean): RestClient {
        this.retryConnectionTimeout = retryConnectionTimeout
        return this
    }

    private fun initHost(): HttpHost {
        var host: String? = (System.getenv() as java.util.Map<String, String>).getOrDefault(
            "ZORROA_ARCHIVIST_URL",
            System.getProperty("system.archivist.url")
        )
        if (host == null) {
            host = "http://localhost:8066"
        }
        val uri = URI.create(host)
        return HttpHost(uri.host, uri.port, uri.scheme)
    }

    private fun initUser(): String {
        return (System.getenv() as java.util.Map<String, String>).getOrDefault(
            "ZORROA_USER",
            System.getProperty("system.user")
        )
            ?: return System.getProperty("user.name")
    }

    private fun initHmacKey(): String? {
        var key: String? = System.getenv("ZORROA_HMAC_KEY")
        if (key != null) {
            return key
        }

        key = System.getProperty("zorroa.hmac.key")
        if (key != null) {
            return key
        }

        val paths = arrayOf(
            String.format(
                "%s/%s.key",
                (System.getenv() as java.util.Map<String, String>).getOrDefault("ZORROA_HMAC_PATH", "/vol/hmac"),
                user
            ),
            String.format("%s/.zorroa/%s.key", System.getProperty("user.home"), user)
        )

        for (path in paths) {
            try {
                key = Files.readAllLines(Paths.get(path))[0]
                break
            } catch (e: IOException) {
            }
        }

        return key
    }

    fun <T> post(url: String, body: Any?, resultType: Class<T>, headers: Map<String, String>? = null): T {
        val post = HttpPost(url)
        if (body != null) {
            post.setHeader("Content-Type", "application/json")
            post.entity = ByteArrayEntity(Json.serialize(body))
        }
        val response = checkStatus(post, headers)
        return checkResponse(response, resultType)
    }

    fun <T> put(url: String, body: Any?, resultType: Class<T>, headers: Map<String, String>? = null): T {
        val post = HttpPut(url)
        if (body != null) {
            post.setHeader("Content-Type", "application/json")
            post.entity = ByteArrayEntity(Json.serialize(body))
        }
        val response = checkStatus(post, headers)
        return checkResponse(response, resultType)
    }

    fun <T> put(url: String, body: Any?, type: TypeReference<T>, headers: Map<String, String>? = null): T {
        val post = HttpPut(url)
        if (body != null) {
            post.setHeader("Content-Type", "application/json")
            post.entity = ByteArrayEntity(Json.serialize(body))
        }
        val response = checkStatus(post, headers)
        return checkResponse(response, type)
    }

    fun <T> post(url: String, body: Any?, type: TypeReference<T>, headers: Map<String, String>? = null): T {
        val post = HttpPost(url)
        if (body != null) {
            post.setHeader("Content-Type", "application/json")
            post.entity = ByteArrayEntity(Json.serialize(body))
        }
        val response = checkStatus(post, headers)
        return checkResponse(response, type)
    }

    fun <T> delete(url: String, body: Any?, resultType: Class<T>, headers: Map<String, String>? = null): T {
        val post = HttpDeleteWithEntity(url)
        if (body != null) {
            post.setHeader("Content-Type", "application/json")
            post.entity = ByteArrayEntity(Json.serialize(body))
        }
        val response = checkStatus(post, headers)
        return checkResponse(response, resultType)
    }

    fun <T> delete(url: String, body: Any?, type: TypeReference<T>, headers: Map<String, String>? = null): T {
        val post = HttpDeleteWithEntity(url)
        if (body != null) {
            post.setHeader("Content-Type", "application/json")
            post.entity = ByteArrayEntity(Json.serialize(body))
        }
        val response = checkStatus(post, headers)
        return checkResponse(response, type)
    }

    fun download(url: String, headers: Map<String, String>? = null): HttpEntity {
        val get = HttpGetWithEntity(url)
        val rsp = checkStatus(get, headers)
        return rsp.entity
    }

    fun post(url: String, files: List<File>, headers: Map<String, String>? = null): Any {

        val builder = MultipartEntityBuilder.create()
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        builder.setBoundary("---Content Boundary")
        for (file in files) {
            builder.addBinaryBody(
                "files", file,
                ContentType.DEFAULT_BINARY, filename(file)
            )
        }
        builder.setBoundary("---Content Boundary")

        val post = HttpPost(url)
        post.addHeader("Content-Type", "multipart/mixed; boundary=\"---Content Boundary\"")
        post.entity = builder.build()
        val response = checkStatus(post, headers)
        return checkResponse(response, Any::class.java)
    }

    fun <T> get(url: String, resultType: Class<T>, headers: Map<String, String>? = null): T {
        return checkResponse(checkStatus(HttpGet(url), headers), resultType)
    }

    fun <T> get(url: String, body: Any?, resultType: Class<T>, headers: Map<String, String>? = null): T {
        val get = HttpGetWithEntity(url)
        if (body != null) {
            get.setHeader("Content-Type", "application/json")
            get.entity = ByteArrayEntity(Json.serialize(body))
        }
        return checkResponse(checkStatus(get, headers), resultType)
    }

    fun <T> get(url: String, type: TypeReference<T>, headers: Map<String, String>? = null): T {
        return checkResponse(checkStatus(HttpGet(url), headers), type)
    }

    private fun <T> checkResponse(response: HttpResponse, resultType: Class<T>): T {
        try {
            val text = response.entity.content.bufferedReader().use { it.readText() }
            return Json.Mapper.readValue(text, resultType)
        } catch (e: Exception) {
            throw RestClientException("Failed to deserialize response", e)
        }
    }

    private fun <T> checkResponse(response: HttpResponse, type: TypeReference<T>): T {
        try {
            val text = response.entity.content.bufferedReader().use { it.readText() }
            return Json.Mapper.readValue(text, type)
        } catch (e: Exception) {
            throw RestClientException("Failed to deserialize response", e)
        }
    }

    private fun checkStatus(req: HttpRequest, headers: Map<String, String>?): HttpResponse {
        headers(req, headers)
        var response: HttpResponse
        while (true) {
            try {
                response = client.execute(host, req)
                break
            } catch (e: Exception) {
                if (retryConnectionTimeout) {
                    try {
                        Thread.sleep(5000)
                    } catch (e1: InterruptedException) {
                        throw RestClientException("Failed to execute request: $req", e)
                    }
                } else {
                    /*
                     * This would be some kind of communication error.
                     */
                    throw RestClientException("Failed to execute request: $req", e)
                }
            }
        }

        if (response.statusLine.statusCode != 200) {
            logger.warn(
                "REST response error: {}{} {} {}",
                host.hostName, req.requestLine.uri,
                response.statusLine.reasonPhrase, response.statusLine.statusCode
            )

            val error = checkResponse(response, Json.GENERIC_MAP)
            val message = error.getOrElse("message") {
                "Unknown REST client exception to $host, ${response.statusLine.statusCode} ${response.statusLine.reasonPhrase}"
            }
            throw RestClientException(message.toString(), response.statusLine.statusCode)
        }

        return response
    }

    private fun headers(req: HttpRequest, headers: Map<String, String>?) {
        val msg = UUID.randomUUID().toString()
        req.setHeader("X-Archivist-User", user)

        headers?.forEach {
            req.setHeader(it.key, it.value)
        }

        hmac?.let {
            try {
                req.setHeader("X-Archivist-Data", msg)
                req.setHeader("X-Archivist-Hmac", calculateSigningKey(msg, it))
            } catch (e: Exception) {
                throw RestClientException("Failed to sign request, $e", e)
            }
        }
    }

    class HttpGetWithEntity(uri: String) : HttpEntityEnclosingRequestBase() {

        init {
            this.uri = URI.create(uri)
        }

        override fun getMethod(): String {
            return METHOD_NAME
        }

        companion object {
            const val METHOD_NAME = "GET"
        }
    }

    class HttpDeleteWithEntity(uri: String) : HttpEntityEnclosingRequestBase() {

        init {
            this.uri = URI.create(uri)
        }

        override fun getMethod(): String {
            return METHOD_NAME
        }

        companion object {
            const val METHOD_NAME = "DELETE"
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(RestClient::class.java)

        @Throws(SignatureException::class, NoSuchAlgorithmException::class, InvalidKeyException::class)
        private fun calculateSigningKey(data: String, key: String): String {
            val signingKey = SecretKeySpec(key.toByteArray(), "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(signingKey)
            return Hex.encodeHexString(mac.doFinal(data.toByteArray()))
        }

        fun initClient(): CloseableHttpClient {
            var factory: SSLConnectionSocketFactory? = null

            try {
                val ctx = SSLContexts.custom()
                    .loadTrustMaterial(null, TrustSelfSignedStrategy())
                    .build()
                factory = SSLConnectionSocketFactory(ctx, NoopHostnameVerifier())
            } catch (e: Exception) {
                logger.warn("Failed to initialize SSL config, ", e)
            }

            val requestConfig = RequestConfig.custom().setConnectTimeout(30 * 1000).build()
            return HttpClients.custom()
                .setConnectionManagerShared(true)
                .setSSLSocketFactory(factory)
                .setDefaultRequestConfig(requestConfig)
                .build()
        }

        fun filename(file: File): String {
            val path = file.absolutePath
            return if (!path.contains("/")) {
                path
            } else {
                path.substring(path.lastIndexOf("/") + 1)
            }
        }
    }
}
