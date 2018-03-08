package com.zorroa.archivist.security

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.google.common.collect.Sets.intersection
import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.Permission
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.processor.Source
import com.zorroa.sdk.schema.PermissionSchema
import com.zorroa.sdk.util.Json
import com.zorroa.security.UserAuthed
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCrypt

val GROUP_ADMIN = "group::administrator"

fun getAuthentication(): Authentication {
    return SecurityContextHolder.getContext().authentication
}

fun createPasswordHash(plainPassword: String): String {
    return BCrypt.hashpw(plainPassword, BCrypt.gensalt())
}

fun getUser(): UserAuthed {
    return if (SecurityContextHolder.getContext().authentication == null) {
        throw AuthenticationCredentialsNotFoundException("No login credentials specified")
    } else {
        try {
            SecurityContextHolder.getContext().authentication.principal as UserAuthed
        } catch (e1: ClassCastException) {
            try {
                SecurityContextHolder.getContext().authentication.details as UserAuthed
            } catch (e2: ClassCastException) {
                throw AuthenticationCredentialsNotFoundException("Invalid login creds, UserAuthed not found")
            }

        }

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

fun getUserId(): Int {
    return getUser().id
}

fun hasPermission(vararg perms: String): Boolean {
    val copy = ImmutableSet.copyOf(perms)
    val auth = SecurityContextHolder.getContext().authentication
    if (auth != null) {
        val authorities = auth.authorities
        if (authorities != null) {
            for (g in authorities) {
                if (copy.contains(g.authority)) {
                    return true
                }
            }
        }
    }
    return false
}

fun hasPermission(permIds: Set<Int>?): Boolean {
    if (permIds == null) {
        return true
    }
    if (permIds == null || permIds.isEmpty()) {
        return true
    }
    return if (hasPermission(GROUP_ADMIN)) {
        true
    } else !intersection<Int>(permIds, getPermissionIds()).isEmpty()
}

fun hasPermission(perms: Collection<String>): Boolean {
    return hasPermission(*perms.toTypedArray())
}

/**
 * Return true if the user has permission to a particular type of permission.
 *
 * @param field
 * @param asset
 * @return
 */
fun hasPermission(field: String, asset: Document): Boolean {
    val perms = asset.getAttr("permissions.$field", Json.SET_OF_INTS)
    return hasPermission(perms)
}

/**
 * Test that the current logged in user has the given access
 * with a particular access control list.  Users with group::superuser
 * will always have access.
 *
 * @param acl
 * @param access
 * @return
 */
fun hasPermission(acl: Acl?, access: Access): Boolean {
    if (acl == null) {
        return true
    }
    return if (hasPermission(GROUP_ADMIN)) {
        true
    } else acl.hasAccess(getPermissionIds(), access)
}

fun getPermissionIds(): Set<Int> {
    val result = Sets.newHashSet<Int>()
    for (g in SecurityContextHolder.getContext().authentication.authorities) {
        val p = g as Permission
        result.add(p.id)
    }
    return result
}

fun getPermissionsFilter(): QueryBuilder? {
    return if (hasPermission(GROUP_ADMIN)) {
        null
    } else QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery("permissions.read",
            getPermissionIds()))
}

fun setWritePermissions(source: Source, perms: Collection<Permission>) {
    var ps: PermissionSchema? = source.getAttr("permissions", PermissionSchema::class.java)
    if (ps == null) {
        ps = PermissionSchema()
    }
    ps.write.clear()
    for (p in perms) {
        ps.write.add(p.id)
    }
    source.setAttr("permissions", ps)
}

fun setExportPermissions(source: Source, perms: Collection<Permission>) {
    var ps: PermissionSchema? = source.getAttr("permissions", PermissionSchema::class.java)
    if (ps == null) {
        ps = PermissionSchema()
    }
    ps.export.clear()
    for (p in perms) {
        ps.export.add(p.id)
    }
    source.setAttr("permissions", ps)
}

/**
 * Return true if the user can set the new ACL.
 *
 * This function checks to ensure that A user isn't taking away access they have by accident.
 *
 * @param newAcl
 * @param oldAcl
 */
fun canSetAclOnFolder(newAcl: Acl, oldAcl: Acl, created: Boolean) {
    if (hasPermission(GROUP_ADMIN)) {
        return
    }

    if (created) {
        if (!hasPermission(newAcl, Access.Read)) {
            throw ArchivistWriteException("You cannot create a folder without read access to it.")
        }
    } else {
        /**
         * Here we check to to see if you have read/write/export access already
         * and we don't let you take away access from yourself.
         */
        val hasRead = hasPermission(oldAcl, Access.Read)
        val hasWrite = hasPermission(oldAcl, Access.Write)
        val hasExport = hasPermission(oldAcl, Access.Export)

        if (hasRead && !hasPermission(newAcl, Access.Read)) {
            throw ArchivistWriteException("You cannot remove read access from yourself.")
        }

        if (hasWrite && !hasPermission(newAcl, Access.Write)) {
            throw ArchivistWriteException("You cannot remove write access from yourself.")
        }

        if (hasExport && !hasPermission(newAcl, Access.Export)) {
            throw ArchivistWriteException("You cannot remove export access from yourself.")
        }
    }
}

/**
 * Return true if the current user can export an asset.
 *
 * @param asset
 * @return
 */
fun canExport(asset: Document): Boolean {
    if (hasPermission("group::export")) {
        return true
    }

    val perms = asset.getAttr("permissions.export", Json.SET_OF_INTS)
    return hasPermission(perms)
}

