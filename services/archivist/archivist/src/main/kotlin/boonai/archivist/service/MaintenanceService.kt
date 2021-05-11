package boonai.archivist.service

import com.google.common.util.concurrent.AbstractScheduledService
import boonai.archivist.domain.AnalystState
import boonai.archivist.repository.JobDao
import boonai.common.service.logging.MeterRegistryHolder.meterRegistry
import io.micrometer.core.instrument.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.random.Random

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
    lateinit var analystRemoveInactivityTime: String

    /**
     * The amount of time to wait for a ping before considering the task an orphan.
     */
    lateinit var taskOrphanTime: String

    /**
     * Return a [Duration] instance from the analystInactivityTimeDown property.
     */
    fun getAnalystDownInactivityTime(): Duration {
        return Duration.parse("PT${analystDownInactivityTime.toUpperCase()}")
    }

    /**
     * Return a [Duration] instance from the analystInactivityTimeRemove property.
     */
    fun getAnalystRemoveInactivityTime(): Duration {
        return Duration.parse("PT${analystRemoveInactivityTime.toUpperCase()}")
    }

    /**
     * Return a [Duration] instance describing the task orphan time.
     */
    fun getTaskOrphanTime(): Duration {
        return Duration.parse("PT${taskOrphanTime.toUpperCase()}")
    }
}

/**
 * A scheduler for resuming jobs where the job pause timer has expired.
 */
@Component
class ResumePausedJobsScheduler @Autowired constructor(
    val jobDao: JobDao,
    val config: MaintenanceConfiguration
) : AbstractScheduledService(), ApplicationListener<ContextRefreshedEvent> {

    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        if (config.enabled) {
            this.startAsync()
        }
    }

    override fun runOneIteration() {
        try {
            jobDao.resumePausedJobs()
        } catch (e: Exception) {
            logger.warn("Failed to unlock paused jobs, ", e)
        }
    }

    override fun scheduler(): Scheduler {
        return Scheduler.newFixedDelaySchedule(
            Random.nextLong(1000, 20000), 5000, TimeUnit.MILLISECONDS
        )
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ResumePausedJobsScheduler::class.java)
    }
}

@Component
class MaintenanceServiceImpl @Autowired constructor(
    val jobService: JobService,
    val dispatcherService: DispatcherService,
    val analystService: AnalystService,
    val config: MaintenanceConfiguration
) : AbstractScheduledService(), MaintenanceService, ApplicationListener<ContextRefreshedEvent> {

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
        handleExpiredJobs()
        handleUnresponsiveAnalysts()
        handleOrphanTasks()
    }

    override fun handleExpiredJobs() {
        try {
            val removedCounter = meterRegistry.counter(
                meterName,
                listOf(Tag.of("event", "job_removed"))
            )
            for (job in jobService.getExpiredJobs(config.archiveJobsAfterDays, TimeUnit.DAYS, 100)) {
                logger.info("Deleting expired job {},", job.id)
                if (jobService.deleteJob(job)) {
                    removedCounter.increment()
                } else {
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
            val downCounter = meterRegistry.counter(
                meterName,
                listOf(Tag.of("event", "analyst_down"))
            )
            analystService.getUnresponsive(AnalystState.Up, downDuration).forEach {
                if (analystService.setState(it, AnalystState.Down)) {
                    analystService.setTaskId(it, null)
                }
                downCounter.increment()
            }
        } catch (e: Exception) {
            logger.warn("Unable to handle unresponsive analysts, ", e)
        }

        try {
            //  get Analysts that Down Up but haven't pinged in for a long time
            val removeDuration = config.getAnalystRemoveInactivityTime()
            val removeCounter = meterRegistry.counter(
                meterName,
                listOf(Tag.of("event", "analyst_removed"))
            )
            analystService.getUnresponsive(AnalystState.Down, removeDuration).forEach {
                analystService.delete(it)
                removeCounter.increment()
            }
        } catch (e: Exception) {
            logger.warn("Unable to handle unresponsive analysts, ", e)
        }
    }

    override fun handleOrphanTasks() {
        try {
            val orphanCounter = meterRegistry.counter(
                meterName,
                listOf(Tag.of("event", "task_orphan"))
            )
            // get tasks marked as queued or running but have not pinged in.
            val orphanDuration = config.getTaskOrphanTime()
            jobService.getOrphanTasks(orphanDuration).forEach {
                dispatcherService.retryTask(it, "Orphaned Task")
                orphanCounter.increment()
            }
        } catch (e: Exception) {
            logger.warn("Unable to handle orphan tasks, ", e)
        }
    }

    override fun scheduler(): Scheduler {
        return Scheduler.newFixedDelaySchedule(Random.nextLong(2000, 60000), 1000, TimeUnit.MILLISECONDS)
    }

    companion object {

        /**
         * The Name of the meter for counting events.
         */
        private const val meterName = "zorroa.maintenance"

        private val logger = LoggerFactory.getLogger(MaintenanceServiceImpl::class.java)
    }
}
