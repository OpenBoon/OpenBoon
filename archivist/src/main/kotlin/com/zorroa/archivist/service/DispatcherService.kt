package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.cloud.storage.HttpMethod
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.*
import com.zorroa.archivist.security.*
import com.zorroa.archivist.service.MeterRegistryHolder.getTags
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PostConstruct


interface DispatcherService {
    /**
     * Return a list of waiting [DispatchTask] instances for the given
     * organization, sorted by highest priority first.
     *
     * @param organizationId: The organization ID.
     * @param count: The maximium number of tasks to return.
     */
    fun getWaitingTasks(organizationId: UUID, count: Int) : List<DispatchTask>
    fun startTask(task: InternalTask): Boolean
    fun stopTask(task: InternalTask, event: TaskStoppedEvent): Boolean
    fun handleEvent(event: TaskEvent)
    fun handleTaskError(task: InternalTask, error: TaskErrorEvent)
    fun expand(parentTask: InternalTask, script: ZpsScript): Task
    fun expand(job: JobId, script: ZpsScript): Task
    fun retryTask(task: InternalTask, reason: String): Boolean
    fun skipTask(task: InternalTask): Boolean
    fun queueTask(task: DispatchTask, endpoint: String): Boolean

    /**
     * Return the [Organization] dispatch priority.
     */
    fun getDispatchPriority() : List<DispatchPriority>
}

/**
 * A non-transactional class for queuing tasks.
 */
@Component
class DispatchQueueManager @Autowired constructor(
        val dispatcherService: DispatcherService,
        val analystService: AnalystService,
        val fileStorageService: FileStorageService,
        val userDao: UserDao,
        val properties: ApplicationProperties,
        val meterRegistry: MeterRegistry
)
{

    /**
     * The number of tasks to poll per request.  A higher number lowers dispatch collisions.
     */
    val numberOfTasksToPoll = 5

    /**
     * The number of seconds before timing out the task priority.
     */
    val cachedDispatchPriorityTimeoutSeconds = 10L

    /**
     * Caches a task priority list which is currently just a list of organizations
     * sorted by the least number of tasks running first.
     */
    val cachedDispatchPriority: Supplier<List<DispatchPriority>> = Suppliers.memoizeWithExpiration({
        dispatcherService.getDispatchPriority()
    }, cachedDispatchPriorityTimeoutSeconds, TimeUnit.SECONDS)

    fun getNext(): DispatchTask? {

        val endpoint = getAnalystEndpoint()
        if (analystService.isLocked(endpoint)) {
            return null
        }

        meterRegistry.counter(
                METRICS_KEY, "op", "requests").increment()

        for (priority in cachedDispatchPriority.get()) {

            val tasks = dispatcherService.getWaitingTasks(
                    priority.organizationId, numberOfTasksToPoll)

            meterRegistry.counter(
                    METRICS_KEY, "op", "tasks-polled").increment(tasks.size.toDouble())

            for (task in tasks) {
                if (dispatcherService.queueTask(task, endpoint)) {
                    meterRegistry.counter(
                            METRICS_KEY, "op", "tasks-queued").increment()

                    task.env["ZORROA_TASK_ID"] = task.id.toString()
                    task.env["ZORROA_JOB_ID"] = task.jobId.toString()
                    task.env["ZORROA_ORGANIZATION_ID"] = task.organizationId.toString()
                    task.env["ZORROA_ARCHIVIST_MAX_RETRIES"] = "0"
                    task.env["ZORROA_AUTH_TOKEN"] =
                            generateUserToken(task.userId, userDao.getHmacKey(task.userId))
                    if (properties.getBoolean("archivist.debug-mode.enabled")) {
                        task.env["ZORROA_DEBUG_MODE"] = "true"
                    }
                    withAuth(SuperAdminAuthentication(task.organizationId)) {
                        val fs = fileStorageService.get(task.getLogSpec())
                        val logFile = fileStorageService.getSignedUrl(
                                fs.id, HttpMethod.PUT, 1, TimeUnit.DAYS)
                        task.logFile = logFile
                    }
                    return task
                }
                else {
                    meterRegistry.counter(METRICS_KEY, "op", "tasks-collided").increment()
                }
            }
        }
        return null
    }

    companion object {

        /**
         * Metrics key used for Dispatch Queue metrics
         */
        private const val METRICS_KEY = "zorroa.dispatch-queue"
    }
}

@Service
@Transactional
class DispatcherServiceImpl @Autowired constructor(
        private val dispatchTaskDao: DispatchTaskDao,
        private val taskDao: TaskDao,
        private val jobDao: JobDao,
        private val taskErrorDao: TaskErrorDao,
        private val analystDao: AnalystDao,
        private val eventBus: EventBus,
        private val meterRegistry: MeterRegistry) : DispatcherService {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var fileStorageService: FileStorageService

    @Autowired
    lateinit var analystService: AnalystService

    @PostConstruct
    fun init() {
        // Register for event bus
        eventBus.register(this)

        meterRegistry.gauge("zorroa.task.pending", AtomicLong()) {
            taskDao.getPendingTaskCount().toDouble()
        }
    }

    @Transactional(readOnly = true)
    override fun getDispatchPriority() : List<DispatchPriority> {
        return meterRegistry.timer("zorroa.dispatch-service.prioritize")
                .record<List<DispatchPriority>> {
            dispatchTaskDao.getDispatchPriority()
        }
    }

    @Transactional(readOnly = true)
    override fun getWaitingTasks(organizationId: UUID, count: Int) : List<DispatchTask> {
        return dispatchTaskDao.getNext(organizationId, count)
    }

    override fun queueTask(task: DispatchTask, endpoint: String): Boolean {
        val result = taskDao.setState(task, TaskState.Queued, TaskState.Waiting)
        return if (result) {
            taskDao.setHostEndpoint(task, endpoint)
            analystDao.setTaskId(endpoint, task.taskId)
            true
        } else {
            false
        }
    }

    override fun startTask(task: InternalTask): Boolean {
        val result = taskDao.setState(task, TaskState.Running, TaskState.Queued)
        if (result) {
            taskDao.resetAssetCounters(task)
            jobDao.setTimeStarted(task)
        }
        logger.info("Starting task: {}, {}", task.taskId, result)
        return result
    }

    override fun stopTask(task: InternalTask, event: TaskStoppedEvent): Boolean {

        val newState = when {
            event.newState != null -> event.newState
            event.exitStatus != 0 -> {
                if (!event.manualKill && taskDao.isAutoRetryable(task)) {
                    TaskState.Waiting
                }
                else {
                    TaskState.Failure
                }
            }
            else -> TaskState.Success
        }

        /**
         * TODO: make sure the task is the right instance of the task, in
         * case it was orphaned and then relaunched, but the orphan came back.
         */
        val stopped = when {
            jobService.setTaskState(task, newState, TaskState.Running) -> true
            jobService.setTaskState(task, newState, TaskState.Queued) -> true
            else -> false
        }

        if (stopped) {
            taskDao.setExitStatus(task, event.exitStatus)
            try {
                val endpoint = getAnalystEndpoint()
                analystDao.setTaskId(endpoint, null)
            } catch (e: Exception) {
                logger.warn("Failed to clear taskId from Analyst")
            }

            if (!event.manualKill && event.exitStatus != 0 && newState == TaskState.Failure) {
                val script = taskDao.getScript(task.taskId)
                val assetCount = script.over?.size ?: 0
                jobService.incrementAssetCounters(task, AssetCounters(errors=assetCount))

                taskErrorDao.batchCreate(task, script.over?.map {
                    TaskErrorEvent(UUID.fromString(it.id),
                            it.getAttr("source.path"),
                            "Hard Task failure, exit ${event.exitStatus}",
                            "unknown",
                            true,
                            "unknown")
                }.orEmpty())
            }
        }

        logger.info("Stopping task: {}, newState={}, result={}", task.taskId, newState, stopped)
        return stopped
    }

    override fun expand(parentTask: InternalTask, script: ZpsScript): Task {

        val parentScript = taskDao.getScript(parentTask.taskId)
        script.globals = parentScript.globals
        script.type = parentScript.type
        script.settings = parentScript.settings

        if (script.execute.orEmpty().isEmpty()) {
            script.execute = parentScript.execute
        }

        val newTask = taskDao.create(parentTask, TaskSpec(zpsTaskName(script), script))
        logger.event(LogObject.JOB, LogAction.EXPAND,
                mapOf("parentTaskId" to parentTask.taskId,
                        "taskId" to newTask.id,
                        "jobId" to newTask.jobId))
        return newTask
    }

    override fun expand(job: JobId, script: ZpsScript): Task {
        val newTask = taskDao.create(job, TaskSpec(zpsTaskName(script), script))
        logger.event(LogObject.JOB, LogAction.EXPAND,
                mapOf("jobId" to newTask.jobId, "taskId" to newTask.id))
        return newTask
    }

    override fun handleTaskError(task: InternalTask, error: TaskErrorEvent) {
        taskErrorDao.create(task, error)
        jobService.incrementAssetCounters(task, AssetCounters(errors=1))
    }

    override fun handleEvent(event: TaskEvent) {
        val task = taskDao.getInternal(event.taskId)
        when (event.type) {
            TaskEventType.STOPPED -> {
                val payload = Json.Mapper.convertValue<TaskStoppedEvent>(event.payload)
                stopTask(task, payload)
            }
            TaskEventType.STARTED -> startTask(task)
            TaskEventType.ERROR -> {
                val payload = Json.Mapper.convertValue<TaskErrorEvent>(event.payload)
                handleTaskError(task, payload)
            }
            TaskEventType.EXPAND -> {
                val script = Json.Mapper.convertValue<ZpsScript>(event.payload)
                expand(task, script)
            }
            TaskEventType.MESSAGE -> {
                val message = Json.Mapper.convertValue<TaskMessageEvent>(event.payload)
                logger.warn("Task Event Message: ${task.taskId} : $message")
            }
        }
    }

    fun killRunningTaskOnAnalyst(task: TaskId, newState: TaskState, reason: String): Boolean {
        val host = taskDao.getHostEndpoint(task)
        if (host == null) {
            logger.warn("Failed to kill running task $task, no host is set")
            return false
        }
        // If we can't kill on the analyst for any reason, then set to Waiting.
        return analystService.killTask(host, task.taskId, reason, newState)
    }

    override fun retryTask(task: InternalTask, reason: String): Boolean {
        meterRegistry.counter("zorroa.task.retry", getTags()).increment()
        return if (task.state.isDispatched()) {
            GlobalScope.launch(Dispatchers.IO) {
                if(!killRunningTaskOnAnalyst(task, TaskState.Waiting, reason)) {
                    logger.warn("Manually setting task {} to Waiting", task)
                    jobService.setTaskState(task, TaskState.Waiting, null)
                }
            }
            // just assuming true here as the call to the analyst is backgrounded
            true
        } else {
            jobService.setTaskState(task, TaskState.Waiting, null)
        }
    }

    override fun skipTask(task: InternalTask): Boolean {
        return if (task.state.isDispatched()) {
            GlobalScope.launch {
                killRunningTaskOnAnalyst(task, TaskState.Skipped, "Task skipped")
            }
            // just assuming true here as the call to the analyst is backgrounded
            true
        } else {
            jobService.setTaskState(task, TaskState.Skipped, null)
        }
    }

    @Subscribe
    fun handleJobStateChangeEvent(event: JobStateChangeEvent) {
        val auth = getAuthentication()
        if (event.newState == JobState.Cancelled) {
            GlobalScope.launch {
                withAuth(auth) {
                    handleJobCanceled(event.job)
                }
            }
        }
    }

    fun handleJobCanceled(job: Job) {
        for (task in taskDao.getAll(job.id, TaskState.Running)) {
            killRunningTaskOnAnalyst(task, TaskState.Waiting, "Job canceled by ")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DispatcherServiceImpl::class.java)
    }
}
