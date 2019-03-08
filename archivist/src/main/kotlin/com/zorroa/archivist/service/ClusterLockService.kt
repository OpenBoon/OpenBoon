package com.zorroa.archivist.service

import com.zorroa.archivist.domain.ClusterLockSpec
import com.zorroa.archivist.domain.LockStatus
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.repository.ClusterLockDao
import com.zorroa.archivist.security.getAuthentication
import com.zorroa.archivist.security.withAuth
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ThreadContextElement
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.concurrent.ListenableFuture
import java.util.concurrent.Callable
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

interface ClusterLockExecutor {

    /**
     * Run the given code inline with the current thread.
     * 
     * @param spec  A ClusterLock spec
     * @param body The code to execute
     */
    fun <T : Any?> inline(spec: ClusterLockSpec, body: () -> T?) : T?

    /**
     * Execute the given function with the named cluster lock.
     *
     * @param spec  A ClusterLock spec
     * @param body The code to execute
     */
    fun <T : Any?> submit(spec: ClusterLockSpec, body: () -> T?) : ListenableFuture<T?>

    /**
     * Execute the given function with the named cluster lock.
     *
     * @param spec A ClusterLock spec
     * @param body The code to execute
     */
    fun execute(spec: ClusterLockSpec, body: () -> Unit)
}


interface ClusterLockService {

    /**
     * Take out a lock on the given name. The lock will persist until it times out or
     * released.
     *
     * @param spec The lock specification
     */
    fun lock(spec: ClusterLockSpec) : LockStatus

    /**
     * Unlock the given lock.
     *
     * @param name The name of the lock
     */
    fun unlock(spec: ClusterLockSpec) : Boolean

    /**
     * Return true if the given lock is locked
     *
     * @param name The name of the lock
     */
    fun isLocked(name: String) : Boolean

    /**
     * Clear all expired locks.
     *
     * @return The number of locks cleared.
     */
    fun clearExpired() : Int

    /**
     * Return true of the given lock is combined with a lock with the same name.  If
     * a combine lock cannot be combine once, then it's no longer able to be combined.
     *
     * @param spec The lock specification
     */
    fun combineLocks(spec : ClusterLockSpec): Boolean
}

/**
 * ThreadLocal holder for storing per-thread cluster locks. Used for
 * reentrant locking.
 */
object ClusterLockContext {
    private val locks : ThreadLocal<MutableList<String>> =
            ThreadLocal.withInitial { mutableListOf<String>() }

    fun getLocks() : MutableList<String>  {
        return locks.get()
    }

    fun setLocks(ctx: MutableList<String>) {
        locks.set(ctx)
    }
}


/**
 * A Coroutine Context for passing around thread local cluster locks.
 *
 * @property locks: A list of cluster locks.
 */
class ClusterLocksCoroutineContext(
        private var locks: MutableList<String>) : ThreadContextElement<MutableList<String>> {

    companion object Key : CoroutineContext.Key<ClusterLocksCoroutineContext>

    override val key: CoroutineContext.Key<ClusterLocksCoroutineContext>
        get() = Key

    override fun updateThreadContext(context: CoroutineContext): MutableList<String> {
        val old = ClusterLockContext.getLocks()
        ClusterLockContext.setLocks(locks)
        return old
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: MutableList<String>) {
        ClusterLockContext.setLocks(oldState)
    }
}


@Component
class ClusterLockExecutorImpl @Autowired constructor(
        val clusterLockService : ClusterLockService,
        val workQueue: AsyncListenableTaskExecutor
): ClusterLockExecutor {

    val maxBackoff = 10000L
    val backoffIncrement = 100L

    override fun <T> inline(spec: ClusterLockSpec, body: () -> T?) : T? {
        var result: T? = null

        if (obtainLock(spec)) {
            try {
                do {
                    try {
                        result = body()
                    } catch (e: Exception) {
                        logger.warn("Failed background cluster task: ${spec.name}", e)
                    }
                } while (clusterLockService.combineLocks(spec))
            } finally {
                clusterLockService.unlock(spec)
            }
        }
        return result
    }

    override fun execute(spec: ClusterLockSpec, body: () -> Unit) {
        val authentication = spec.authentication ?: getAuthentication()
        workQueue.execute {
            withAuth(authentication) {
                if (obtainLock(spec)) {
                    try {
                        do {
                            try {
                                body()
                            } catch (e: Exception) {
                                logger.warn("Failed background cluster task: ${spec.name}", e)
                            }
                        } while (clusterLockService.combineLocks(spec))
                    } finally {
                        clusterLockService.unlock(spec)
                    }
                }
            }
        }
    }

    override fun <T> submit(spec: ClusterLockSpec, body: () -> T?) : ListenableFuture<T?> {
        val authentication =  spec.authentication ?: getAuthentication()
        return workQueue.submitListenable(Callable {
            withAuth(authentication) {
                if (!obtainLock(spec)) {
                    null
                }
                var result: T?
                try {
                    do {
                        result = body()
                    } while (clusterLockService.combineLocks(spec))
                } finally {
                    clusterLockService.unlock(spec)
                }
                result
            }
        })
    }

    fun obtainLock(spec: ClusterLockSpec) : Boolean {


        var tryNum = 0
        var backOff = backoffIncrement

        var lock: LockStatus
        while (true) {
            lock = clusterLockService.lock(spec)
            if (lock == LockStatus.Wait) {
                tryNum += 1
                if (spec.maxTries != -1 && tryNum >= spec.maxTries) {
                    logger.warnEvent(LogObject.CLUSTER_LOCK, LogAction.LOCK,
                            "Unable to obtain lock ${spec.name} after $tryNum/${spec.maxTries} tries")
                    break
                }
                logger.event(LogObject.CLUSTER_LOCK, LogAction.BACKOFF,
                        mapOf("backoffMs" to backOff, "trynum" to tryNum))

                Thread.sleep(backOff)
                backOff = min(maxBackoff, backOff + backoffIncrement)
            } else {
                break
            }
        }
        return lock == LockStatus.Locked
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterLockExecutorImpl::class.java)
    }
}

@Service
@Transactional
class ClusterLockServiceImpl @Autowired constructor(
        val clusterLockDao: ClusterLockDao,
        val meterRegistry: MeterRegistry
): ClusterLockService {

    @Transactional(readOnly = true)
    override fun isLocked(name: String): Boolean {
        return clusterLockDao.isLocked(name)
    }

    override fun combineLocks(spec : ClusterLockSpec): Boolean {
        if (!spec.combineMultiple) { return false }
        return clusterLockDao.combineLocks(spec)
    }

    @Transactional(propagation = Propagation.REQUIRED)
    override fun lock(spec : ClusterLockSpec) : LockStatus {
        val ctx = ClusterLockContext.getLocks()
        if (ctx.contains(spec.name)) {
            meterRegistry.counter("zorroa-reentrant-lock", "lockName", spec.name)
            return LockStatus.Locked
        }
        val locked = clusterLockDao.lock(spec)
        if (locked == LockStatus.Locked) {
            ctx.add(spec.name)
        }
        return locked
    }

    @Transactional(propagation = Propagation.REQUIRED)
    override fun unlock(spec: ClusterLockSpec) : Boolean {
        ClusterLockContext.getLocks().remove(spec.name)
        return if (!spec.holdTillTimeout) {
            clusterLockDao.unlock(spec.name)
        } else {
            false
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun clearExpired() : Int {
        return try {
            clusterLockDao.clearExpired()
        } catch (e: Exception) {
            logger.warn("Failed to clear expired cluster locks, ", e)
            0
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterLockServiceImpl::class.java)
    }
}