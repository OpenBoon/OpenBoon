package com.zorroa.auth.security

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.auth.JSON_MAPPER
import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.KeyGenerator
import com.zorroa.auth.domain.ZmlpActor
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.UUID

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
        val key = if (it.length > 100) {
            val decoded = Base64.getUrlDecoder().decode(it)
            JSON_MAPPER.readValue(decoded)
        } else {
            val path = Paths.get(it)
            if (Files.exists(path)) {
                JSON_MAPPER.readValue<ApiKey>(path.toFile())
            } else {
                null
            }
        }
        if (key != null) {
            logger.info("Loading external keyId: ${key.keyId}")
            return key
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