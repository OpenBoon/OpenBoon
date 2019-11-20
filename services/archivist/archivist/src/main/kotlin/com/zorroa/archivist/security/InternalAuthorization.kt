package com.zorroa.archivist.security

import com.zorroa.archivist.clients.ZmlpUser
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

/**
 * An Authentication class for authorizing background threads.
 */
class InternalThreadAuthentication constructor(
    projectId: UUID, perms: List<String>
) :
    AbstractAuthenticationToken(perms.map { SimpleGrantedAuthority(it) }) {

    val zmlpUser: ZmlpUser = ZmlpUser(
        KnownKeys.SUKEY,
        projectId,
        KnownKeys.BACKGROUND_THREAD,
        perms
    )

    init {
        logger.info("switching Auth to Project ${zmlpUser.projectId}")
    }

    override fun getDetails(): Any? {
        return zmlpUser
    }

    override fun getCredentials(): Any? {
        return zmlpUser.projectId
    }

    override fun getPrincipal(): Any {
        return zmlpUser
    }

    override fun isAuthenticated(): Boolean {
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InternalThreadAuthentication::class.java)
    }
}
