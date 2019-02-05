package com.zorroa.archivist.service

import com.google.common.util.concurrent.AbstractScheduledService
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.archivist.security.withAuth
import com.zorroa.common.domain.AnalystState
import com.zorroa.common.domain.TaskState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
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
     * Run all Maintenance.  Return true if lock was obtained, false if not.
     */
    fun runAll() : Boolean
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
}


@Component
class MaintenanceServiceImpl @Autowired constructor(
        val storageService: FileStorageService,
        val jobService: JobService,
        val analystService: AnalystService,
        val clusterLockService: ClusterLockService,
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

    override fun runAll() : Boolean {
        clusterLockService.clearExpired()

        val locked = clusterLockService.lock(lockName, 90, TimeUnit.MINUTES)
        if (locked) {
            try {
                handleExpiredJobs()
                handleUnresponsiveAnalysts()
            }
            finally {
                clusterLockService.unlock(lockName)
            }
        }
        return locked
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
            for (analyst in analystService.getUnresponsive(AnalystState.Up, 5, TimeUnit.MINUTES)) {
                if (analystService.setState(analyst, AnalystState.Down)) {
                    logger.event(LogObject.ANALYST, LogAction.STATE_CHANGE,
                            mapOf("newState" to AnalystState.Down,
                                    "oldState" to AnalystState.Up,
                                    "reason" to "unresponsive"))

                    // Try to talk to the analyst anyway
                    analyst.taskId?.let {
                        if (!analystService.killTask(analyst.endpoint, it, "Analyst went down", TaskState.Waiting)) {
                            val task = jobService.getTask(analyst.taskId)
                            jobService.setTaskState(task, TaskState.Waiting, TaskState.Running)
                            analystService.setTaskId(analyst, null)
                        }
                    }
                }
            }

            // get ones that have been down for a long time.
            for (analyst in analystService.getUnresponsive(AnalystState.Down, 1, TimeUnit.HOURS)) {
                logger.event(LogObject.ANALYST, LogAction.DELETE,
                        mapOf("reason" to "unresponsive"))
                analystService.delete(analyst)
            }

        } catch (e: Exception) {
            logger.warn("Unable to handle unresponsive analysts, ", e)
        }
    }

    override fun scheduler(): AbstractScheduledService.Scheduler {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(5, 1, TimeUnit.MINUTES)
    }

    companion object {

        private const val lockName = "maintenance"

        private val logger = LoggerFactory.getLogger(MaintenanceServiceImpl::class.java)
    }
}
