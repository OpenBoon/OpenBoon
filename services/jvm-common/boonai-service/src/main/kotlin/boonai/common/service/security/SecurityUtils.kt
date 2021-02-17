package boonai.common.service.security

import boonai.common.apikey.ZmlpActor
import java.util.UUID
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Base64
import java.util.concurrent.ThreadLocalRandom

fun getAuthentication(): Authentication? {
    return SecurityContextHolder.getContext().authentication
}

fun getZmlpActor(): ZmlpActor {
    val auth = SecurityContextHolder.getContext().authentication
    return if (auth == null) {
        throw SecurityException("No credentials")
    } else {
        try {
            auth.principal as ZmlpActor
        } catch (e: java.lang.ClassCastException) {
            throw SecurityException("Invalid credentials", e)
        }
    }
}

fun getZmlpActorOrNull(): ZmlpActor? {
    return try {
        getZmlpActor()
    } catch (ex: Exception) {
        null
    }
}

fun getProjectId(): UUID {
    return getZmlpActor().projectId
}

object KeyGenerator {
    /**
     * Generate a random base64 encoded string.
     */
    fun generate(size: Int): String {
        val random = ThreadLocalRandom.current()
        val r = ByteArray(size)
        random.nextBytes(r)
        return Base64.getUrlEncoder().encodeToString(r).trimEnd('=')
    }
}
