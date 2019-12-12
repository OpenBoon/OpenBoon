package com.zorroa.auth.security

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.auth.JSON_MAPPER
import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.KeyGenerator
import com.zorroa.auth.domain.ZmlpActor
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder

private val logger = LoggerFactory.getLogger("com.zorroa.auth.security")

fun getProjectId(): UUID {
    val auth = SecurityContextHolder.getContext().authentication
    val user = auth.principal as ZmlpActor
    return user.projectId
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
            val key = JSON_MAPPER.readValue<ApiKey>(path.toFile())
            logger.info("Loaded Inception key: ${key.keyId.prefix(8)} from: '$path'")
            key
        } else {
            try {
                val decoded = Base64.getUrlDecoder().decode(it)
                val key = JSON_MAPPER.readValue<ApiKey>(decoded)
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
        "random", listOf("SuperAdmin")
    )
}

/**
 * Extension function for printing UUID chars
 */
fun UUID.prefix(size: Int = 8): String {
    return this.toString().substring(0, size)
}
