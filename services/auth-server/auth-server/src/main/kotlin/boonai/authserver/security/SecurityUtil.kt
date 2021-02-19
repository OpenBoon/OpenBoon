package boonai.authserver.security

import com.fasterxml.jackson.module.kotlin.readValue
import boonai.authserver.domain.ValidationKey
import boonai.common.apikey.ZmlpActor
import boonai.common.util.Json
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder

private val logger = LoggerFactory.getLogger("boonai.authserver")

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
fun loadServiceKey(serviceKey: String?): ValidationKey {
    serviceKey?.let {
        val path = Paths.get(it)
        val apikey = if (Files.exists(path)) {
            val key = Json.Mapper.readValue<ValidationKey>(path.toFile())
            logger.info("Loaded Inception key: ${key.accessKey.substring(8)} from: '$path'")
            key
        } else if (!it.startsWith("/")) {
            try {
                val decoded = Base64.getUrlDecoder().decode(it)
                val key = Json.Mapper.readValue<ValidationKey>(decoded)
                logger.info("Loaded Inception key: ${key.accessKey.substring(8)}")
                key
            } catch (e: Exception) {
                logger.warn("Failed to load inception key, decode failed, unexpected", e)
                null
            }
        } else {
            null
        }

        if (apikey != null) {
            return apikey
        }
    }

    throw RuntimeException(
        "Unable to load inception key, " +
            "check the boonai.security.inception-key property."
    )
}

/**
 * Generates API signing keys.
 */
object KeyGenerator {
    fun generate(size: Int): String {
        val random = ThreadLocalRandom.current()
        val r = ByteArray(size)
        random.nextBytes(r)
        return Base64.getUrlEncoder().encodeToString(r).trimEnd('=')
    }
}
