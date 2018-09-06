package com.zorroa.archivist.service

import com.google.common.util.concurrent.AbstractScheduledService
import com.zorroa.archivist.repository.SharedLinkDao
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Created by chambers on 4/21/16.
 */
interface MaintenanceService {

    fun removeExpiredSharedLinks(): Int
}

@Service
class MaintenanceServiceImpl @Autowired constructor(
        private val sharedLinkDao: SharedLinkDao
) : AbstractScheduledService(), MaintenanceService, ApplicationListener<ContextRefreshedEvent> {


    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        this.startAsync()
    }

    override fun removeExpiredSharedLinks(): Int {
        val result = sharedLinkDao.deleteExpired(System.currentTimeMillis())
        if (result > 0) {
            logger.info("deleted {} shared link records", result)
        }
        return result
    }

    @Throws(Exception::class)
    override fun runOneIteration() {

        /**
         * Remove expired shared links.
         */
        removeExpiredSharedLinks()

    }

    override fun scheduler(): AbstractScheduledService.Scheduler {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(1, 60, TimeUnit.MINUTES)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MaintenanceServiceImpl::class.java)
    }
}
