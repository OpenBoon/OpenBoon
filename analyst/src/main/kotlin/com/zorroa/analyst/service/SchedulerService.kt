package com.zorroa.analyst.service

import com.zorroa.analyst.scheduler.K8SchedulerServiceImpl
import com.zorroa.common.domain.Job
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct
import kotlin.concurrent.fixedRateTimer
import io.fabric8.kubernetes.api.model.Job as KJob

interface SchedulerService {

    /**
     * Start running the given job.
     */
    fun runJob(job: Job) : Boolean

    /**
     * Do a scheduling pass.
     */
    fun schedule()

    /**
     * Retry the given job.
     */
    fun retry(job: Job) : Boolean

    /**
     * Kill the given job and set to the failed state.
     */
    fun kill(job: Job) : Boolean
}


@Configuration
@ConfigurationProperties("analyst.scheduler")
class SchedulerProperties {
    var type: String? = "local"
    var maxJobs : Int = 3
    var maxJobAgeHours = 24
    var k8 : Map<String, Any>? = null
}

@Component
class MasterScheduler {

    @Autowired
    lateinit var scheduler: SchedulerService

    /**
     * A fixedRateTimer
     */
    private var timer : Timer? = null

    /**
     * Determines if the scheduler is paused or not
     */
    private val paused = AtomicBoolean(false)

    @PostConstruct
    fun start() {
        logger.info("Initializing scheduler timer")
        timer = fixedRateTimer("scheduler",
                daemon = true, initialDelay = 10000, period = 3000) {
            if (!paused.get()) {
                try {
                    scheduler.schedule()
                } catch (e: Exception) {
                    logger.warn("Scheduler failed to run: {}", e)
                }
            }
        }
    }

    fun pause() : Boolean = paused.compareAndSet(false, true)

    fun resume() : Boolean = paused.compareAndSet(true, false)

    companion object {
        private val logger = LoggerFactory.getLogger(K8SchedulerServiceImpl::class.java)
    }
}
