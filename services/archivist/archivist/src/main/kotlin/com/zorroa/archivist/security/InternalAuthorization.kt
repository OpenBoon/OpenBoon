package com.zorroa.archivist.security

import com.zorroa.auth.client.Permission
import com.zorroa.auth.client.ZmlpActor
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

/**
 * An Authentication class for authorizing background threads.
 */
class InternalThreadAuthentication constructor(
    projectId: UUID, perms: Set<Permission> = setOf()
) :
    AbstractAuthenticationToken(perms.map { SimpleGrantedAuthority(it.name) }) {

    val zmlpActor: ZmlpActor = ZmlpActor(
        KnownKeys.SUKEY,
        projectId,
        KnownKeys.BACKGROUND_THREAD,
        perms
    )

    init {
        logger.info("switching Auth to Project ${zmlpActor.projectId}")
    }

    override fun getDetails(): Any? {
        return zmlpActor
    }

    override fun getCredentials(): Any? {
        return zmlpActor.projectId
    }

    override fun getPrincipal(): Any {
        return zmlpActor
    }

    override fun isAuthenticated(): Boolean {
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InternalThreadAuthentication::class.java)
    }
}
