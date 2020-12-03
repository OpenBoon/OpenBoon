package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchIndexAssetsEvent
import com.zorroa.archivist.domain.BatchIndexResponse
import com.zorroa.archivist.domain.DispatchPriority
import com.zorroa.archivist.domain.DispatchTask
import com.zorroa.archivist.domain.InternalTask
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobPriority
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.JobStateChangeEvent
import com.zorroa.archivist.domain.PendingTasksStats
import com.zorroa.archivist.domain.ProjectDirLocator
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.TaskErrorEvent
import com.zorroa.archivist.domain.TaskEvent
import com.zorroa.archivist.domain.TaskEventType
import com.zorroa.archivist.domain.TaskExpandEvent
import com.zorroa.archivist.domain.TaskId
import com.zorroa.archivist.domain.TaskMessageEvent
import com.zorroa.archivist.domain.TaskProgressUpdateEvent
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.domain.TaskStatsEvent
import com.zorroa.archivist.domain.TaskStatusUpdateEvent
import com.zorroa.archivist.domain.TaskStoppedEvent
import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.repository.DispatchTaskDao
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.security.InternalThreadAuthentication
import com.zorroa.archivist.security.KnownKeys
import com.zorroa.archivist.security.getAnalyst
import com.zorroa.archivist.security.getAuthentication
import com.zorroa.archivist.security.withAuth
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.zmlp.apikey.AuthServerClient
import com.zorroa.zmlp.apikey.Permission
import com.zorroa.zmlp.service.logging.MeterRegistryHolder.getTags
import com.zorroa.zmlp.service.logging.MeterRegistryHolder.meterRegistry
import com.zorroa.zmlp.service.storage.SystemStorageService
import com.zorroa.zmlp.util.Json
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

    /**
     * Handle [BatchIndexAssetsEvent] from Analyst.  Updates the assets
     * and adds any ES errors to the error log.
     */
    fun handleIndexEvent(task: InternalTask, event: BatchIndexAssetsEvent): BatchIndexResponse

    fun expand(parentTask: InternalTask, event: TaskExpandEvent): Task?
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

    /**
     * Update Task Progress
     */
    fun handleProgressUpdateEvent(taskId: TaskId, taskProgressUpdateEvent: TaskProgressUpdateEvent)

    /**
     * Update Task Status
     */
    fun handleStatusUpdateEvent(taskId: TaskId, taskStatusUpdateEvent: TaskStatusUpdateEvent)

    /**
     * Number of pending Tasks and Maximum Running Tasks allowed
     */
    fun getPendingTasksStats(): PendingTasksStats
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
    val jobService: JobService,
    val systemStorageService: SystemStorageService,
    val assetService: AssetService,
    val storageService: ProjectStorageService
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
    val cachedDispatchPriority: Supplier<List<DispatchPriority>> = Suppliers.memoizeWithExpiration(
        {
            dispatcherService.getDispatchPriority()
        },
        cachedDispatchPriorityTimeoutSeconds, TimeUnit.SECONDS
    )

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

            fetchTaskEnvironment()?.let {
                task.env.putAll(it)
            }

            task.env["ZMLP_TASK_ID"] = task.id.toString()
            task.env["ZMLP_JOB_ID"] = task.jobId.toString()
            task.env["ZMLP_PROJECT_ID"] = task.projectId.toString()
            task.dataSourceId?.let { task.env["ZMLP_DATASOURCE_ID"] = it.toString() }
            task.env["ZMLP_ARCHIVIST_MAX_RETRIES"] = "0"

            // So the container can make API calls as the JobRunner
            // This call is made with inception key
            val key = authServerClient.getSigningKey(task.projectId, KnownKeys.JOB_RUNNER)
            task.env["ZMLP_APIKEY"] = key.toBase64()
            task.env["ZMLP_CREDENTIALS_TYPES"] = jobService.getCredentialsTypes(task).joinToString(",")

            withAuth(InternalThreadAuthentication(task.projectId, setOf())) {
                task.logFile = storageService.getSignedUrl(
                    task.getLogFileLocation(), true, 1, TimeUnit.DAYS
                ).getValue("uri").toString()

                task.env["ZORROA_JOB_STORAGE_PATH"] =
                    ProjectDirLocator(ProjectStorageEntity.JOB, task.jobId.toString()).getPath()
            }

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

    /**
     * Fetches an external base task environment from system storage.
     */
    fun fetchTaskEnvironment(): Map<String, String>? {
        return try {
            systemStorageService.fetchObject("environments/task_env.json", Json.ENV_MAP)
        } catch (e: Exception) {
            null
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DispatchQueueManager::class.java)

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
    private val assetService: AssetService
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

    @Transactional(readOnly = true)
    override fun getPendingTasksStats(): PendingTasksStats {
        return dispatchTaskDao.getPendingTasksStats()
    }

    override fun queueTask(task: DispatchTask, endpoint: String): Boolean {
        val result = taskDao.setState(task, TaskState.Queued, TaskState.Waiting)
        return if (result) {
            taskDao.setHostEndpoint(task, endpoint)
            analystDao.setTaskId(endpoint, task.taskId)

            // If the task is queued with asset IDs then
            // resolve the asset Ids.
            withAuth(
                InternalThreadAuthentication(
                    task.projectId,
                    setOf(Permission.AssetsRead)
                )
            ) {

                task.script.assetIds?.let {
                    val assets = assetService.getAll(it)
                    task.script.assets = assets
                }
            }
            true
        } else {
            false
        }
    }

    override fun startTask(task: InternalTask): Boolean {
        val result = taskDao.setState(task, TaskState.Running, TaskState.Queued)
        if (result) {
            taskErrorDao.deleteAll(task as TaskId)
            jobDao.setTimeStarted(task)
        }
        logger.info("Starting task: {}, {}", task.taskId, result)
        return result
    }

    override fun stopTask(task: InternalTask, event: TaskStoppedEvent): Boolean {
        val newState = when {
            event.newState != null -> event.newState
            event.exitStatus != 0 -> {
                // Auto-retry only happens on hard failures.
                if (!event.manualKill &&
                    event.exitStatus == EXIT_STATUS_HARD_FAIL &&
                    taskDao.isAutoRetryable(task)
                ) {
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

            if (!event.manualKill && event.exitStatus == EXIT_STATUS_HARD_FAIL &&
                newState == TaskState.Failure
            ) {

                taskErrorDao.create(
                    task,
                    TaskErrorEvent(
                        null,
                        null,
                        "Hard task container failure, all assets failed, exit ${event.exitStatus},",
                        "unknown",
                        true,
                        "unknown"
                    )
                )
            }
        }

        logger.info("Stopping task: {}, newState={}, result={}", task.taskId, newState, stopped)
        return stopped
    }

    override fun expand(parentTask: InternalTask, event: TaskExpandEvent): Task? {

        val result = assetService.batchCreate(
            BatchCreateAssetsRequest(event.assets, analyze = false, task = parentTask)
        )

        taskErrorDao.batchCreate(
            parentTask,
            result.failed.map {
                TaskErrorEvent(
                    it.assetId,
                    it.uri ?: "unknown",
                    it.message,
                    "unknown",
                    true,
                    "index"
                )
            }
        )

        return assetService.createAnalysisTask(parentTask, result.created, result.exists)
    }

    override fun handleTaskError(task: InternalTask, error: TaskErrorEvent) {
        taskErrorDao.create(task, error)
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

    override fun handleIndexEvent(task: InternalTask, event: BatchIndexAssetsEvent): BatchIndexResponse {

        val errors = mutableListOf<TaskErrorEvent>()

        val result = withAuth(
            InternalThreadAuthentication(
                task.projectId,
                setOf(Permission.AssetsImport)
            )
        ) {
            val result = assetService.batchIndex(event.assets, true)
            result.failed.forEach {
                val asset = Asset(it.assetId, event.assets[it.assetId] ?: mutableMapOf())
                val error = TaskErrorEvent(
                    it.assetId,
                    asset.getAttr("source.path"),
                    it.message,
                    "unknown",
                    true,
                    "index"
                )
                errors.add(error)
                taskErrorDao.batchCreate(task, errors)
            }
            result
        }

        return result
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
                withAuth(
                    InternalThreadAuthentication(
                        task.projectId,
                        setOf(Permission.AssetsImport)
                    )
                ) {
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
                val index = Json.Mapper.convertValue<BatchIndexAssetsEvent>(event.payload)
                handleIndexEvent(task, index)
            }
            TaskEventType.PROGRESS -> {
                val progress = Json.Mapper.convertValue<TaskProgressUpdateEvent>(event.payload)
                handleProgressUpdateEvent(task, progress)
            }
            TaskEventType.STATUS -> {
                val status = Json.Mapper.convertValue<TaskStatusUpdateEvent>(event.payload)
                handleStatusUpdateEvent(task, status)
            }
        }
    }

    override fun handleProgressUpdateEvent(taskId: TaskId, taskProgressUpdateEvent: TaskProgressUpdateEvent) {
        taskDao.setProgress(taskId, taskProgressUpdateEvent.progress)
        if (taskProgressUpdateEvent.status != null) {
            taskDao.setStatus(taskId, taskProgressUpdateEvent.status)
        }
    }

    override fun handleStatusUpdateEvent(taskId: TaskId, taskStatusUpdateEvent: TaskStatusUpdateEvent) {
        taskDao.setStatus(taskId, taskStatusUpdateEvent.status)
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
        logger.info("Handling job state changes event.")
        val auth = getAuthentication()
        if (event.newState == JobState.Cancelled) {
            GlobalScope.launch(Dispatchers.IO) {
                withAuth(auth) {
                    handleJobCanceled(event.job)
                }
            }
        }
    }

    fun handleJobCanceled(job: Job) {
        val tasks = taskDao.getAll(job.id, TaskState.Running)
        logger.info("Killing ${tasks.size} tasks on job ${job.id} / ${job.name}")
        tasks.forEach {
            killRunningTaskOnAnalyst(it, TaskState.Waiting, "Job canceled by ")
        }
    }

    companion object {

        // If the task fails with a 9, its a hard failure and an error gets generated
        // for all the assets.
        const val EXIT_STATUS_HARD_FAIL = 9

        private val logger = LoggerFactory.getLogger(DispatcherServiceImpl::class.java)
    }
}
