package com.zorroa.auth.server.security

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.auth.client.Json
import com.zorroa.auth.client.Permission
import com.zorroa.auth.client.ZmlpActor
import com.zorroa.auth.client.prefix
import com.zorroa.auth.server.domain.ApiKey
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder

private val logger = LoggerFactory.getLogger("com.zorroa.auth.server")

fun getProjectId(): UUID {
    val auth = SecurityContextHolder.getContext().authentication
    val user = auth.principal as ZmlpActor
    return user.projectId
}

fun getZmlpActor(): ZmlpActor {
    val auth = SecurityContextHolder.getContext().authentication
    return auth.principal as ZmlpActor
}

/**
 * Load inception service key fom the given string.  The
 * string can be a file or base64 encoded string.  A random
 * key is generated if there is no service key defined.
 *
 * @param serviceKey: The string, or null.
 */
fun loadServiceKey(serviceKey: String?): ApiKey {
    serviceKey?.let {
        val path = Paths.get(it)
        val apikey = if (Files.exists(path)) {
            val key = Json.mapper.readValue<ApiKey>(path.toFile())
            logger.info("Loaded Inception key: ${key.keyId.prefix(8)} from: '$path'")
            key
        } else {
            try {
                val decoded = Base64.getUrlDecoder().decode(it)
                val key = Json.mapper.readValue<ApiKey>(decoded)
                logger.info("Loaded Inception key: ${key.keyId.prefix(8)}")
                key
            } catch (e: Exception) {
                logger.warn("Failed to load inception key, decode failed, unexpected", e)
                null
            }
        }

        if (apikey != null) {
            return apikey
        }
    }

    logger.info("Generating RANDOM service key")
    return ApiKey(
        UUID.randomUUID(),
        UUID.randomUUID(),
        KeyGenerator.generate() + KeyGenerator.generate(),
        "random", setOf(Permission.SystemMonitor.name)
    )
}

/**
 * Generates API signing keys.
 */
object KeyGenerator {
    fun generate(): String {
        val random = ThreadLocalRandom.current()
        val r = ByteArray(64)
        random.nextBytes(r)
        return Base64.getUrlEncoder().encodeToString(r).trimEnd('=')
    }
}
