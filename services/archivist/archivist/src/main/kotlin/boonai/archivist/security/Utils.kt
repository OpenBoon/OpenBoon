package boonai.archivist.security

import boonai.common.apikey.Permission
import boonai.common.apikey.ZmlpActor
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
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

fun getAnalyst(): AnalystAuthentication {
    val auth = SecurityContextHolder.getContext().authentication
    return if (auth is AnalystAuthentication) {
        auth
    } else {
        throw AuthenticationCredentialsNotFoundException("No login credentials specified for cluster node")
    }
}

fun hasPermission(vararg perms: Permission): Boolean {
    val strPerms = perms.map { it.name }
    val auth = SecurityContextHolder.getContext().authentication
    auth?.authorities?.let { authorities ->
        for (g in authorities) {
            if (strPerms.contains(g.authority)) {
                return true
            }
        }
    }
    return false
}

fun hasPermission(perms: Collection<Permission>): Boolean {
    val strPerms = perms.map { it.name }
    val auth = SecurityContextHolder.getContext().authentication
    auth?.authorities?.let { authorities ->
        for (g in authorities) {
            if (strPerms.contains(g.authority)) {
                return true
            }
        }
    }
    return false
}

fun getProjectFilter(): QueryBuilder {
    return QueryBuilders.termQuery("system.projectId", getZmlpActor().projectId.toString())
}
