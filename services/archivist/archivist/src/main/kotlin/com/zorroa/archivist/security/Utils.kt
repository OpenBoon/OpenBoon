package com.zorroa.archivist.security

import com.zorroa.archivist.clients.ApiKey
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCrypt
import java.util.UUID

object Role {
    val SUPERADMIN = "ROLE_SUPERADMIN"
    val MONITOR = "ROLE_MONITOR"
}

/**
 * Set a new Authentication value and return the previous one, or null in the case
 * that no Authentication exists.
 *
 * @param auth The new Authentication value.
 * @return the old authentication value or null
 */
fun resetAuthentication(auth: Authentication?): Authentication? {
    val oldAuth = SecurityContextHolder.getContext().authentication
    SecurityContextHolder.getContext().authentication = auth
    return oldAuth
}

/**
 * Execute the given code using the provided Authentication object.  If Authentication
 * is null, then just execute with no authentication.
 *
 */
fun <T> withAuth(auth: Authentication?, body: () -> T): T {
    val hold = SecurityContextHolder.getContext().authentication
    SecurityContextHolder.getContext().authentication = auth
    try {
        return body()
    } finally {
        SecurityContextHolder.getContext().authentication = hold
    }
}

object SecurityLogger {
    val logger = LoggerFactory.getLogger(SecurityLogger::class.java)
}

fun getAuthentication(): Authentication? {
    return SecurityContextHolder.getContext().authentication
}

fun getSecurityContext(): SecurityContext {
    return SecurityContextHolder.getContext()
}

fun createPasswordHash(plainPassword: String): String {
    return BCrypt.hashpw(plainPassword, BCrypt.gensalt())
}

/**
 * Generate a alpha-numeric random password of the given length.
 *
 * @param length The password length
 * @return A random password.
 */
fun generateRandomPassword(length: Int): String {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length).map { allowedChars.random() }.joinToString("")
}

fun getApiKey(): ApiKey {
    val auth = SecurityContextHolder.getContext().authentication
    return if (auth == null) {
        throw SecurityException("No credentials")
    }
    else {
        try {
            auth.principal as ApiKey
        }
        catch (e: java.lang.ClassCastException) {
            throw SecurityException("Invalid credentials", e)
        }
    }
}

fun getApiKeyOrNull(): ApiKey? {
    return try {
        getApiKey()
    } catch (ex: Exception) {
        null
    }
}

fun getProjectId() : UUID {
    return getApiKey().projectId
}

fun getAnalystEndpoint(): String {
    val auth = SecurityContextHolder.getContext().authentication
    return if (auth == null) {
        throw AuthenticationCredentialsNotFoundException("No login credentials specified for cluster node")
    } else {
        return SecurityContextHolder.getContext().authentication.principal as String
    }
}

fun hasPermission(vararg perms: String): Boolean {
    return hasPermission(perms.toSet())
}

private fun containsOnlySuperadmin(perms: Collection<String>): Boolean {
    return perms.isNotEmpty() and perms.all { it == Role.SUPERADMIN }
}

private fun containsSuperadmin(it: Collection<GrantedAuthority>) =
    it.any { it.authority == Role.SUPERADMIN }

fun hasPermission(perms: Collection<String>): Boolean {
    val auth = SecurityContextHolder.getContext().authentication
    auth?.authorities?.let { authorities ->
        if (containsSuperadmin(authorities)) {
            return true
        } else if (!containsOnlySuperadmin(perms)) {
            for (g in authorities) {
                if (perms.contains(g.authority)) {
                    return true
                }
            }
        }
    }
    return false
}


fun getProjectFilter(): QueryBuilder {
    return QueryBuilders.termQuery("system.projectId", getApiKey().projectId.toString())
}

