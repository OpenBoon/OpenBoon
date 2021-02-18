package boonai.archivist.security

import kotlinx.coroutines.ThreadContextElement
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.coroutines.CoroutineContext

class CoroutineAuthentication(
    private var securityContext: SecurityContext
) : ThreadContextElement<SecurityContext> {

    constructor() : this(SecurityContextHolder.getContext())

    companion object Key : CoroutineContext.Key<CoroutineAuthentication>

    override val key: CoroutineContext.Key<CoroutineAuthentication>
        get() = Key

    override fun updateThreadContext(context: CoroutineContext): SecurityContext {
        val old = SecurityContextHolder.getContext()
        SecurityContextHolder.setContext(securityContext)
        return old
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: SecurityContext) {
        SecurityContextHolder.setContext(oldState)
    }
}
