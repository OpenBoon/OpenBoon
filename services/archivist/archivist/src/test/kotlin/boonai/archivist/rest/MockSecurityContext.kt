package boonai.archivist.rest

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext

class MockSecurityContext(private var authentication: Authentication?) : SecurityContext {

    override fun getAuthentication(): Authentication? {
        return authentication
    }

    override fun setAuthentication(authentication: Authentication) {
        this.authentication = authentication
    }

    companion object {

        private val serialVersionUID = 1L
    }
}
