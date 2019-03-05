package com.zorroa.archivist.service

import com.zorroa.archivist.domain.ClusterLockSpec
import com.zorroa.archivist.domain.LockStatus
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.repository.ClusterLockDao
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.math.min

interface ClusterLockExecutor {
    /**
     * Execute the given function with the named cluster lock.
     *
     * @param spec  A ClusterLock spec
     * @param body The code to execute
     */
    fun <T : Any?> async(spec: ClusterLockSpec, body: () -> T?) : Deferred<T?>

    /**
     * Execute the given function with the named cluster lock.
     *
     * @param spec A ClusterLock spec
     * @param body The code to execute
     */
    fun launch(spec: ClusterLockSpec, body: () -> Unit) : Job
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
    fun unlock(name: String) : Boolean

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

@Component
class ClusterLockExecutorImpl @Autowired constructor(
        val clusterLockService : ClusterLockService
): ClusterLockExecutor {

    val maxBackoff = 10000L
    val backoffIncrement = 100L

    override fun launch(spec: ClusterLockSpec, body: () -> Unit)= GlobalScope.launch {
        if(obtainLock(spec, coroutineContext)) {
            try {
                do {
                    try {
                        body()
                    } catch (e: Exception) {
                        logger.warn("Failed background cluster task: ${spec.name}", e)
                    }
                } while(clusterLockService.combineLocks(spec))
            }
            finally {
                if (!spec.holdTillTimeout) {
                    clusterLockService.unlock(spec.name)
                }
            }
        }
    }

    override fun <T> async(spec: ClusterLockSpec, body: () -> T?) : Deferred<T?> {
        return GlobalScope.async {
            if (!obtainLock(spec, coroutineContext)) {
               null
            }
            var result : T?
            try {
                do {
                    result = body()
                } while(clusterLockService.combineLocks(spec))
            }
            finally {
                if (!spec.holdTillTimeout) {
                    clusterLockService.unlock(spec.name)
                }
            }

            result
        }
    }

    suspend fun obtainLock(spec: ClusterLockSpec, context: CoroutineContext) : Boolean {
        return withContext(context) {
            var tryNum = 0
            var backOff = backoffIncrement

            var lock : LockStatus
            while (true) {
                lock = clusterLockService.lock(spec)
                if (lock == LockStatus.Wait) {
                    tryNum+=1
                    if (spec.maxTries != -1 && tryNum >= spec.maxTries) {
                        logger.warnEvent(LogObject.CLUSTER_LOCK, LogAction.LOCK,
                                "Unable to obtain lock ${spec.name} after $tryNum/${spec.maxTries} tries")
                        break
                    }
                    logger.event(LogObject.CLUSTER_LOCK, LogAction.BACKOFF,
                            mapOf("backoffMs" to backOff, "trynum" to tryNum))

                    delay(backOff)
                    backOff=min(maxBackoff, backOff+backoffIncrement)
                }
                else {
                    break
                }
            }
            lock == LockStatus.Locked
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterLockExecutorImpl::class.java)
    }
}


@Service
@Transactional
class ClusterLockServiceImpl @Autowired constructor(
        val clusterLockDao: ClusterLockDao
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
        return clusterLockDao.lock(spec)
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun unlock(name: String) : Boolean {
        return clusterLockDao.unlock(name)
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