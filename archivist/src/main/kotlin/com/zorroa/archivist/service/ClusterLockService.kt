package com.zorroa.archivist.service

import com.zorroa.archivist.repository.ClusterLockDao
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

interface ClusterLockService {
    fun lock(name: String, duration: Long, unit: TimeUnit) : Boolean
    fun unlock(name: String) : Boolean
    fun clearExpired() : Int
}

@Service
@Transactional
class ClusterLockServiceImpl @Autowired constructor(
        val clusterLockDao: ClusterLockDao
): ClusterLockService {


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun lock(name: String, duration: Long, unit: TimeUnit) : Boolean {
        return clusterLockDao.lock(name, duration, unit)
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