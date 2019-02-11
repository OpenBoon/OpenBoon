package com.zorroa.archivist.service

import com.zorroa.archivist.repository.ClusterLockDao
import com.zorroa.archivist.security.getAuthentication
import com.zorroa.archivist.security.withAuth
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

interface ClusterLockExecutor {
    fun <T : Any?> runAsync(name: String, body: () -> T?) : T?
    fun run(name: String, body: () -> Unit) : Job
    suspend fun obtainLock(name: String, context: CoroutineContext, maxTries:Int=128) : Boolean
}


interface ClusterLockService {
    fun lock(name: String, timeout: Long=1, unit: TimeUnit=TimeUnit.MINUTES) : Boolean
    fun unlock(name: String) : Boolean
    fun isLocked(name: String) : Boolean
    fun clearExpired() : Int
    fun shouldRunAgain(name: String) : Boolean
}

@Component
class ClusterLockExecutorImpl @Autowired constructor(
        val clusterLockService : ClusterLockService
): ClusterLockExecutor {

    val maxBackoff = 1000L
    val backoffIncrement = 100L

    override fun run(name: String, body: () -> Unit)= GlobalScope.launch {
        if(obtainLock(name, coroutineContext)) {
            try {
                body()
            } catch (e: Exception) {
                logger.warn("Failed cluster task: $name", e)
            } finally {
                clusterLockService.unlock(name)
            }
        }
    }

    override fun <T> runAsync(name: String, body: () -> T?) = runBlocking {
        val res = async {
            if (!obtainLock(name, coroutineContext)) {
               null
            }
            val result = try {
                body()
            }
            finally {
                clusterLockService.unlock(name)
            }
            result
        }
        res.await()
    }

    override suspend fun obtainLock(name: String, context: CoroutineContext, maxTries:Int) : Boolean {
        return withContext(context) {
            var tryNum = 0
            var backOff = backoffIncrement
            // Put a lock in with a timeout of 60 seconds, so the lock will disappear
            // eventually if the server crashes.
            while(!clusterLockService.lock(name, 60, TimeUnit.SECONDS)) {
                tryNum+=1
                if (tryNum > maxTries) {
                    logger.warn("Unable to obtain lock $name after $tryNum tries")
                    false
                }
                logger.warn("Unable to obtain lock $name, backoff #$tryNum for ${backOff}ms")
                delay(backOff)
                backOff=min(maxBackoff, backOff+backoffIncrement)
            }
            true
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

    @Transactional(propagation = Propagation.REQUIRED)
    override fun lock(name: String, timeout: Long, unit: TimeUnit) : Boolean {
        return clusterLockDao.lock(name, timeout, unit)
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun unlock(name: String) : Boolean {
        return clusterLockDao.unlock(name)
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun shouldRunAgain(name: String) : Boolean {
        return clusterLockDao.shouldRunAgain(name)
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