package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.zorroa.archivist.clients.AuthServerClient
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.DispatchPriority
import com.zorroa.archivist.domain.DispatchTask
import com.zorroa.archivist.domain.IndexAssetsEvent
import com.zorroa.archivist.domain.InternalTask
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobPriority
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.JobStateChangeEvent
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.STANDARD_PIPELINE
import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.TaskErrorEvent
import com.zorroa.archivist.domain.TaskEvent
import com.zorroa.archivist.domain.TaskEventType
import com.zorroa.archivist.domain.TaskExpandEvent
import com.zorroa.archivist.domain.TaskId
import com.zorroa.archivist.domain.TaskMessageEvent
import com.zorroa.archivist.domain.TaskSpec
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.domain.TaskStatsEvent
import com.zorroa.archivist.domain.TaskStoppedEvent
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.repository.DispatchTaskDao
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.security.InternalThreadAuthentication
import com.zorroa.archivist.security.KnownKeys
import com.zorroa.archivist.security.Perm
import com.zorroa.archivist.security.getAnalyst
import com.zorroa.archivist.security.getAuthentication
import com.zorroa.archivist.security.withAuth
import com.zorroa.archivist.service.MeterRegistryHolder.getTags
import com.zorroa.archivist.storage.SharedStorageServiceConfiguration
import com.zorroa.archivist.util.Json
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

interface DispatcherService {
    /**
     * Return a list of waiting [DispatchTask] instances for the given
     * project, sorted by highest priority first.
     *
     * @param projectId: The projectId ID.
     * @param count: The maximium number of tasks to return.
     */
    fun getWaitingTasks(projectId: UUID, count: Int): List<DispatchTask>

    /**
     * Return a list of waiting [DispatchTask] instances with at least
     * the minimum priority.  Project is not taken into account.
     * Tasks are pre-sorted by highest priortity first.
     *
     * @param minPriority The minimum task priority.
     * @param count The maximum number of tasks to return.
     */
    fun getWaitingTasks(minPriority: Int, count: Int): List<DispatchTask>

    fun startTask(task: InternalTask): Boolean
    fun stopTask(task: InternalTask, event: TaskStoppedEvent): Boolean
    fun handleEvent(event: TaskEvent)
    fun handleTaskError(task: InternalTask, error: TaskErrorEvent)
    fun expand(parentTask: InternalTask, event: TaskExpandEvent): Task
    fun retryTask(task: InternalTask, reason: String): Boolean
    fun skipTask(task: InternalTask): Boolean
    fun queueTask(task: DispatchTask, endpoint: String): Boolean

    /**
     * Return the Project dispatch priority.
     */
    fun getDispatchPriority(): List<DispatchPriority>

    /**
     * Add per-processor runtime stats provided by the Analyst event system
     * to the meterRegistry.
     */
    fun handleStatsEvent(stats: List<TaskStatsEvent>)
}

/**
 * A non-transactional class for queuing tasks.
 */
@Component
class DispatchQueueManager @Autowired constructor(
    val dispatcherService: DispatcherService,
    val analystService: AnalystService,
    val properties: ApplicationProperties,
    val authServerClient: AuthServerClient,
    val sharedStoragProperties: SharedStorageServiceConfiguration,
    val meterRegistry: MeterRegistry
) {

    /**
     * The number of tasks to poll per request.  A higher number lowers dispatch collisions.
     */
    val numberOfTasksToPoll = 5

    /**
     * The number of seconds before timing out the task priority.
     */
    val cachedDispatchPriorityTimeoutSeconds = 10L

    /**
     * Caches a task priority list which is currently just a list of projects
     * sorted by the least number of tasks running first.
     */
    val cachedDispatchPriority: Supplier<List<DispatchPriority>> = Suppliers.memoizeWithExpiration({
        dispatcherService.getDispatchPriority()
    }, cachedDispatchPriorityTimeoutSeconds, TimeUnit.SECONDS)

    /**
     * Return the next available dispatchable [DispatchTask] or null if there are not any.
     */
    fun getNext(): DispatchTask? {

        val analyst = getAnalyst()
        if (analystService.isLocked(analyst.endpoint)) {
            return null
        }

        meterRegistry.counter(
            METRICS_KEY, "op", "requests"
        ).increment()

        /**
         * First check for interactive jobs.
         */
        val tasks = dispatcherService.getWaitingTasks(JobPriority.Interactive, numberOfTasksToPoll)
        meterRegistry.counter(
            METRICS_KEY, "op", "tasks-polled"
        ).increment(tasks.size.toDouble())

        for (task in tasks) {
            if (queueAndDispatchTask(task, analyst.endpoint)) {
                return task
            }
        }

        /**
         * If no interactive jobs can be found, pull tasks by project with
         * least number of running tasks first.
         */
        for (priority in cachedDispatchPriority.get()) {

            val waitingTasks = dispatcherService.getWaitingTasks(
                priority.projectId, numberOfTasksToPoll
            )

            meterRegistry.counter(
                METRICS_KEY, "op", "tasks-polled"
            ).increment(tasks.size.toDouble())

            for (task in waitingTasks) {
                if (queueAndDispatchTask(task, analyst.endpoint)) {
                    return task
                }
            }
        }

        return null
    }

    /**
     * Attempt to queue and dispatch a given task. Return true
     * if the task is dispatched, false if not.
     *
     * @param task The task to dispatch
     * @param analyst The hostname for the [Analyst] asking for a task.
     */
    fun queueAndDispatchTask(task: DispatchTask, analyst: String): Boolean {
        if (queueTask(task, analyst)) {
            meterRegistry.counter(
                METRICS_KEY, "op", "tasks-queued"
            ).increment()

            task.env["ZMLP_TASK_ID"] = task.id.toString()
            task.env["ZMLP_JOB_ID"] = task.jobId.toString()
            task.env["ZMLP_PROJECT_ID"] = task.projectId.toString()
            task.dataSourceId?.let { task.env["ZMLP_DATASOURCE_ID"] = it.toString() }
            task.env["ZMLP_ARCHIVIST_MAX_RETRIES"] = "0"

            // So the container can make API calls as the JobRunner
            val key = authServerClient.getApiKey(task.projectId, KnownKeys.JOB_RUNNER)
            task.env["ZMLP_APIKEY"] = key.toBase64()

            // So the container can access shared
            task.env["ZMLP_MLSTORAGE_URL"] = sharedStoragProperties.url
            task.env["ZMLP_MLSTORAGE_ACCESSKEY"] = sharedStoragProperties.accessKey
            task.env["ZMLP_MLSTORAGE_SECRETKEY"] = sharedStoragProperties.secretKey

            return true
        } else {
            meterRegistry.counter(METRICS_KEY, "op", "tasks-collided").increment()
        }

        return false
    }

    /**
     * The queueTask method handles the fact that dispatcherService.queueTask may throw
     * a DataIntegrityViolationException.  It's better to let the exception propagate
     * out to this point than to catch it in a @Transactional class because
     * the transaction is invalid anyway.
     */
    fun queueTask(task: DispatchTask, analyst: String): Boolean {
        return try {
            dispatcherService.queueTask(task, analyst)
        } catch (e: DataIntegrityViolationException) {
            false
        }
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
    private val assetService: AssetService,
    private val meterRegistry: MeterRegistry
) : DispatcherService {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var analystService: AnalystService

    @PostConstruct
    fun init() {
        // Register for event bus
        eventBus.register(this)
    }

    @Transactional(readOnly = true)
    override fun getDispatchPriority(): List<DispatchPriority> {
        return meterRegistry.timer("zorroa.dispatch-service.prioritize")
            .record<List<DispatchPriority>> {
                dispatchTaskDao.getDispatchPriority()
            }
    }

    @Transactional(readOnly = true)
    override fun getWaitingTasks(projectId: UUID, count: Int): List<DispatchTask> {
        return dispatchTaskDao.getNextByProject(projectId, count)
    }

    @Transactional(readOnly = true)
    override fun getWaitingTasks(minPriority: Int, count: Int): List<DispatchTask> {
        return dispatchTaskDao.getNextByJobPriority(minPriority, count)
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
                } else {
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
                val endpoint = getAnalyst().endpoint
                analystDao.setTaskId(endpoint, null)
            } catch (e: Exception) {
                logger.warn("Failed to clear taskId from Analyst")
            }

            if (!event.manualKill && event.exitStatus != 0 && newState == TaskState.Failure) {
                val script = taskDao.getScript(task.taskId)
                val assetCount = script.assets?.size ?: 0

                // TODO: part of job and asset stats
                //jobService.incrementAssetCounters(task, AssetCounters(errors = assetCount))

                taskErrorDao.batchCreate(task, script.assets?.map {
                    TaskErrorEvent(
                        it.id,
                        it.getAttr("source.path"),
                        "Hard Task failure, exit ${event.exitStatus}",
                        "unknown",
                        true,
                        "unknown"
                    )
                }.orEmpty())
            }
        }

        logger.info("Stopping task: {}, newState={}, result={}", task.taskId, newState, stopped)
        return stopped
    }

    override fun expand(parentTask: InternalTask, event: TaskExpandEvent): Task {

        val result = assetService.batchCreate(
            BatchCreateAssetsRequest(event.assets, analyze = false, task=parentTask)
        )

        val name = "Expand ${result.status.size} assets"
        val parentScript = taskDao.getScript(parentTask.taskId)
        val newScript = ZpsScript(name, null, result.assets, STANDARD_PIPELINE)

        newScript.globalArgs = parentScript.globalArgs
        newScript.type = parentScript.type
        newScript.settings = parentScript.settings

        val newTask = taskDao.create(parentTask, TaskSpec(name, newScript))
        logger.event(
            LogObject.JOB, LogAction.EXPAND,
            mapOf(
                "assetCount" to event.assets.size,
                "parentTaskId" to parentTask.taskId,
                "taskId" to newTask.id,
                "jobId" to newTask.jobId
            )
        )
        return newTask
    }

    override fun handleTaskError(task: InternalTask, error: TaskErrorEvent) {
        taskErrorDao.create(task, error)
        // TODO: part of job stats update
        //jobService.incrementAssetCounters(task, AssetCounters(errors = 1))
    }

    override fun handleStatsEvent(stats: List<TaskStatsEvent>) {
        for (stat in stats) {
            val proc = stat.processor
            meterRegistry.timer("zorroa.processor.process_min_time", "processor", proc)
                .record(stat.min.times(1000).toLong(), TimeUnit.MILLISECONDS)
            meterRegistry.timer("zorroa.processor.process_max_time", "processor", proc)
                .record(stat.max.times(1000).toLong(), TimeUnit.MILLISECONDS)
            meterRegistry.timer("zorroa.processor.process_avg_time", "processor", proc)
                .record(stat.avg.times(1000).toLong(), TimeUnit.MILLISECONDS)
        }
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
                withAuth(InternalThreadAuthentication(task.projectId, listOf(Perm.ASSETS_WRITE))) {
                    val payload = Json.Mapper.convertValue<TaskExpandEvent>(event.payload)
                    expand(task, payload)
                }
            }
            TaskEventType.MESSAGE -> {
                val message = Json.Mapper.convertValue<TaskMessageEvent>(event.payload)
                logger.warn("Task Event Message: ${task.taskId} : $message")
            }
            TaskEventType.STATS -> {
                val stats = Json.Mapper.convertValue<List<TaskStatsEvent>>(event.payload)
                handleStatsEvent(stats)
            }
            TaskEventType.INDEX -> {
                val index = Json.Mapper.convertValue<IndexAssetsEvent>(event.payload)
                withAuth(InternalThreadAuthentication(task.projectId, listOf(Perm.ASSETS_WRITE))) {
                    assetService.batchUpdate(BatchUpdateAssetsRequest(index.assets))
                }
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
                if (!killRunningTaskOnAnalyst(task, TaskState.Waiting, reason)) {
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
