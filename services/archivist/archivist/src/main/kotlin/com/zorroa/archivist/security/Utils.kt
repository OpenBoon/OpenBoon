package com.zorroa.archivist.security

import com.zorroa.archivist.domain.Groups
import com.zorroa.archivist.domain.UserAuthed
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

fun getUser(): UserAuthed {
    val auth = SecurityContextHolder.getContext().authentication
    return if (auth == null) {
        throw AuthenticationCredentialsNotFoundException("No login credentials specified")
    } else {
        try {
            auth.principal as UserAuthed
        } catch (e1: ClassCastException) {
            try {
                auth.details as UserAuthed
            } catch (e2: ClassCastException) {
                // Log this message so we can see what the type is.
                SecurityLogger.logger.warn(
                    "Invalid auth objects: principal='{}' details='{}'",
                    auth?.principal, auth?.details
                )
                throw AuthenticationCredentialsNotFoundException("Invalid auth object, UserAuthed object not found")
            }
        }
    }
}

fun getAnalystEndpoint(): String {
    val auth = SecurityContextHolder.getContext().authentication
    return if (auth == null) {
        throw AuthenticationCredentialsNotFoundException("No login credentials specified for cluster node")
    } else {
        return SecurityContextHolder.getContext().authentication.principal as String
    }
}

fun getUserOrNull(): UserAuthed? {
    return try {
        getUser()
    } catch (ex: AuthenticationCredentialsNotFoundException) {
        null
    }
}

fun getUsername(): String {
    return getUser().username
}

fun getUserId(): UUID {
    return getUser().id
}

fun getOrgId(): UUID {
    return getUser().organizationId
}

fun hasPermission(vararg perms: String): Boolean {
    return hasPermission(perms.toSet())
}

private fun containsOnlySuperadmin(perms: Collection<String>): Boolean {
    return perms.isNotEmpty() and perms.all { it == Groups.SUPERADMIN }
}

private fun containsSuperadmin(it: Collection<GrantedAuthority>) =
    it.any { it.authority == Groups.SUPERADMIN }

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


fun getOrganizationFilter(): QueryBuilder {
    return QueryBuilders.termQuery("system.organizationId", getOrgId().toString())
}

