package com.zorroa.archivist.service

import com.google.common.util.concurrent.AbstractScheduledService
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.MaintenanceDao
import com.zorroa.archivist.repository.SharedLinkDao
import com.zorroa.archivist.sdk.config.ApplicationProperties
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Created by chambers on 4/21/16.
 */
interface MaintenanceService {

    /**
     * Utilize the archivist.maintenance.jobs.expireDays setting to
     * remove logs and tasks for expired jobs.  The job record is kept
     * around for reference.
     *
     * @return
     */
    fun removeExpiredJobData(): Int

    /**
     * Remove both old tasks and log files that have exceeded the expiration time and return
     * the number of jobs processed.
     */
    fun removeExpiredJobData(olderThan: Long): Int

    fun removeExpiredSharedLinks(): Int
}

@Service
class MaintenanceServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties,
        private val maintenanceDao: MaintenanceDao,
        private val jobDao: JobDao,
        private val sharedLinkDao: SharedLinkDao
) : AbstractScheduledService(), MaintenanceService, ApplicationListener<ContextRefreshedEvent> {


    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        this.startAsync()
    }

    override fun removeExpiredJobData(): Int {
        /**
         * Remove expired jobs.
         */
        try {
            val expireDays = properties.getString("archivist.maintenance.jobs.expireDays")
            val expireTime = System.currentTimeMillis() - Integer.valueOf(expireDays) * 86400000
            return removeExpiredJobData(expireTime)
        } catch (e: Exception) {
            logger.warn("Failed to remove expired job data, ", e)
        }

        return 0
    }

    override fun removeExpiredJobData(olderThan: Long): Int {

        val jobs = maintenanceDao.getExpiredJobs(olderThan)
        for (job in jobs) {
            maintenanceDao.removeTaskData(job)
            /**
             * We keep the job around as a record of how date got into
             * the system, so we just set the state to expired.
             */
            jobDao.setState(job, JobState.Expired, null)
        }

        /**
         * Now we do the disk IO.
         */
        for (job in jobs) {
            try {
                FileUtils.deleteDirectory(File(job.rootPath))
            } catch (e: IOException) {
                logger.warn("Failed to delete directory, '" + job.exportedPath + "', ", e)
            }

        }
        return jobs.size
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
