package com.zorroa.archivist.service

import com.google.common.util.concurrent.AbstractScheduledService
import com.zorroa.archivist.domain.ClusterLockSpec
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.archivist.security.withAuth
import com.zorroa.common.domain.AnalystState
import com.zorroa.common.domain.TaskState
import kotlinx.coroutines.Dispatchers
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Created by chambers on 4/21/16.
 */
interface MaintenanceService {

    /**
     * Handles removing of expired jobs jobs.  Expired jobs have not had any
     * activity for some time. See MaintenanceConfiguration
     */
    fun handleExpiredJobs()

    /**
     * Handles setting Analysts to the down state, and removing analysts that haven't
     * pinged in for some time.
     */
    fun handleUnresponsiveAnalysts()

    /**
     * Handles retrying all tasks that have not pinged in for a set amount of time.
     */
    fun handleOrphanTasks()

    /**
     * Run all Maintenance.  Return true if lock was obtained, false if not.
     */
    fun runAll()
}

@Configuration
@ConfigurationProperties("archivist.maintenance")
class MaintenanceConfiguration {

    /**
     * If maintenance is enabled or not.  Multiple archivists doing maintenance will
     * be an issue until a locking mechanism is created.
     */
    val enabled: Boolean = true

    /**
     * Number of days before inactive jobs are removed.
     */
    var archiveJobsAfterDays: Long = 90

    /**
     * Mark Analyst down after being down for this period of time.
     */
    lateinit var analystDownInactivityTime: String

    /**
     * Remove Analyst from list after being down for this period of time.
     */
    lateinit var analystRemoveInactivityTime : String

    /**
     * The amount of time to wait for a ping before considering the task an orphan.
     */
    lateinit var taskOrphanTime : String

    /**
     * Return a [Duration] instance from the analystInactivityTimeDown property.
     */
    fun getAnalystDownInactivityTime() : Duration {
        return Duration.parse("PT${analystDownInactivityTime.toUpperCase()}")
    }

    /**
     * Return a [Duration] instance from the analystInactivityTimeRemove property.
     */
    fun getAnalystRemoveInactivityTime() : Duration {
        return Duration.parse("PT${analystRemoveInactivityTime.toUpperCase()}")
    }

    /**
     * Return a [Duration] instance describing the task orphan time.
     */
    fun getTaskOrphanTime() : Duration {
        return Duration.parse("PT${taskOrphanTime.toUpperCase()}")
    }

}

/**
 * A scheduler for resuming jobs where the job pause timer has expired.
 */
@Component
class ResumePausedJobsScheduler @Autowired constructor(
        val jobDao: JobDao,
        val clusterLockExecutor: ClusterLockExecutor,
        val config: MaintenanceConfiguration) : AbstractScheduledService(), ApplicationListener<ContextRefreshedEvent> {

    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        if (config.enabled) {
            this.startAsync()
        }
    }

    override fun runOneIteration() {
        val lock = ClusterLockSpec.softLock(lockName).apply {
            timeout = 1
            timeoutUnits = TimeUnit.MINUTES
            dispatcher = Dispatchers.IO
        }
        try {
            clusterLockExecutor.inline(lock) {
                jobDao.resumePausedJobs()
            }
        } catch (e: Exception) {
            logger.warn("Failed to unlock paused jobs, ", e)
        }
    }

    override fun scheduler(): AbstractScheduledService.Scheduler {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(60, 5, TimeUnit.SECONDS)
    }

    companion object {

        private const val lockName = "resume-jobs"

        private val logger = LoggerFactory.getLogger(ResumePausedJobsScheduler::class.java)
    }

}

@Component
class MaintenanceServiceImpl @Autowired constructor(
        val storageService: FileStorageService,
        val jobService: JobService,
        val dispatcherService: DispatcherService,
        val analystService: AnalystService,
        val clusterLockService: ClusterLockService,
        val clusterLockExecutor: ClusterLockExecutor,
        val config: MaintenanceConfiguration) : AbstractScheduledService(), MaintenanceService, ApplicationListener<ContextRefreshedEvent> {


    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        logger.info("MaintenanceService is enabled: {}", config.enabled)
        if (config.enabled) {
            logger.info("MaintenanceService archiving jobs after {}  days", config.archiveJobsAfterDays)
            this.startAsync()
        }
    }

    @Throws(Exception::class)
    override fun runOneIteration() {
        // Don't let anything bubble out of here of the thread dies
        try {
            runAll()
        } catch (e: Exception) {
            logger.warn("Failed to run data maintenance, ", e)
        }
    }

    override fun runAll() {
        val lock = ClusterLockSpec.softLock(lockName).apply {
            timeout = 10
            timeoutUnits = TimeUnit.MINUTES
            dispatcher = Dispatchers.IO
        }

        clusterLockExecutor.inline(lock) {
            clusterLockService.clearExpired()
            handleExpiredJobs()
            handleUnresponsiveAnalysts()
            handleOrphanTasks()
        }
    }

    override fun handleExpiredJobs() {
        try {
            for (job in jobService.getExpiredJobs(config.archiveJobsAfterDays, TimeUnit.DAYS, 100)) {
                logger.info("Deleting expired job {},", job.id)
                if (jobService.deleteJob(job)) {
                    withAuth(SuperAdminAuthentication(job.organizationId)) {
                        val storage = storageService.get(job.getStorageId())
                        storage.getServableFile().delete()
                    }
                }
                else {
                    logger.warn("Failed to delete job $job from DB, did not exist.")
                }
            }
        } catch (e: Exception) {
            logger.warn("Unable to handle expired job data, ", e)
        }
    }

    override fun handleUnresponsiveAnalysts() {
        try {
            //  get Analysts that are Up but haven't pinged in
            val downDuration = config.getAnalystDownInactivityTime()
            analystService.getUnresponsive(AnalystState.Up, downDuration).forEach {
                analystService.setState(it, AnalystState.Down)
                analystService.setTaskId(it, null)
            }
        } catch (e: Exception) {
            logger.warn("Unable to handle unresponsive analysts, ", e)
        }

        try {
            //  get Analysts that Down Up but haven't pinged in for a long time
            val removeDuration = config.getAnalystRemoveInactivityTime()
            analystService.getUnresponsive(AnalystState.Down, removeDuration).forEach {
                analystService.delete(it)
            }

        } catch (e: Exception) {
            logger.warn("Unable to handle unresponsive analysts, ", e)
        }
    }

    override fun handleOrphanTasks() {
        try {
            //  get Analysts that are Up but haven't pinged in
            val orphanDuration = config.getTaskOrphanTime()
            jobService.getOrphanTasks(orphanDuration).forEach {
                dispatcherService.retryTask(it, "Orphaned Task")
            }
        } catch (e: Exception) {
            logger.warn("Unable to handle orphan tasks, ", e)
        }
    }

    override fun scheduler(): AbstractScheduledService.Scheduler {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES)
    }

    companion object {

        private const val lockName = "maintenance"

        private val logger = LoggerFactory.getLogger(MaintenanceServiceImpl::class.java)
    }
}
