package boonai.archivist.security

import boonai.common.apikey.Permission
import boonai.common.apikey.ZmlpActor
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * An Authentication class for authorizing background threads.
 */
class InternalThreadAuthentication constructor(
    projectId: UUID,
    perms: Set<Permission> = setOf(),
    attrs: Map<String, String> = mapOf()
) :
    AbstractAuthenticationToken(perms.map { SimpleGrantedAuthority(it.name) }) {

    val zmlpActor: ZmlpActor = ZmlpActor(
        KnownKeys.SUKEY,
        projectId,
        KnownKeys.BACKGROUND_THREAD,
        perms,
        attrs = attrs.toMutableMap()
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
