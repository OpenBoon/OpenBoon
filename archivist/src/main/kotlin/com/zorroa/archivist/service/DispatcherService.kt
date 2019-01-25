package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.cloud.storage.HttpMethod
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.*
import com.zorroa.archivist.security.*
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

interface DispatcherService {
    fun getWaitingTasks(limit: Int) : List<DispatchTask>
    fun startTask(task: Task): Boolean
    fun stopTask(task: Task, event: TaskStoppedEvent): Boolean
    fun handleEvent(event: TaskEvent)
    fun expand(parentTask: Task, script: ZpsScript): Task
    fun expand(job: JobId, script: ZpsScript): Task
    fun retryTask(task: Task): Boolean
    fun skipTask(task: Task): Boolean
    fun queueTask(task: DispatchTask, endpoint: String): Boolean
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
        val properties: ApplicationProperties
)
{

    fun getNext(): DispatchTask? {
        val endpoint = getAnalystEndpoint()
        if (analystService.isLocked(endpoint)) {
            return null
        }

        if (endpoint != null) {
            val tasks = dispatcherService.getWaitingTasks(10)
            for (task in tasks) {
                if (dispatcherService.queueTask(task, endpoint)) {

                    task.env["ZORROA_TASK_ID"] = task.id.toString()
                    task.env["ZORROA_JOB_ID"] = task.jobId.toString()
                    task.env["ZORROA_AUTH_TOKEN"] = generateUserToken(task.userId, userDao.getHmacKey(task.userId))
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
            }
        }
        return null
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
        private val eventBus: EventBus) : DispatcherService {


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
    }


    @Transactional(readOnly = true)
    override fun getWaitingTasks(limit: Int) : List<DispatchTask> {
        return dispatchTaskDao.getNext(limit)
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

    override fun startTask(task: Task): Boolean {
        val result = taskDao.setState(task, TaskState.Running, TaskState.Queued)
        if (result) {
            jobDao.setTimeStarted(task)
        }
        logger.info("Starting task: {}, {}", task.taskId, result)
        return result
    }

    override fun stopTask(task: Task, event: TaskStoppedEvent): Boolean {

        val newState = when {
            event.newState != null -> event.newState
            event.exitStatus != 0 -> TaskState.Failure
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

            if (!event.manualKill && event.exitStatus != 0) {
                val script = taskDao.getScript(task.id)
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

    override fun expand(parentTask: Task, script: ZpsScript): Task {

        val parentScript = taskDao.getScript(parentTask.id)
        script.globals = parentScript.globals
        script.type = parentScript.type
        script.inline = true
        script.settings = parentScript.settings

        if (script.execute.orEmpty().isEmpty()) {
            script.execute = parentScript.execute
        }

        val newTask = taskDao.create(parentTask, TaskSpec(zpsTaskName(script), script))
        logger.info("Expanding parent task: {} with task: {}", parentTask.id, newTask.id)
        return newTask
    }

    override fun expand(job: JobId, script: ZpsScript): Task {
        val newTask = taskDao.create(job, TaskSpec(zpsTaskName(script), script))
        logger.info("Expanding job: {} with task: {}", job.jobId, newTask.id)
        return newTask
    }

    override fun handleEvent(event: TaskEvent) {
        val task = taskDao.get(event.taskId)
        when (event.type) {
            TaskEventType.STOPPED -> {
                val payload = Json.Mapper.convertValue<TaskStoppedEvent>(event.payload)
                stopTask(task, payload)
            }
            TaskEventType.STARTED -> startTask(task)
            TaskEventType.ERROR -> {
                // Might have to queue and submit in batches
                val payload = Json.Mapper.convertValue<TaskErrorEvent>(event.payload)
                taskErrorDao.create(task, payload)
            }
            TaskEventType.EXPAND -> {
                val task = taskDao.get(task.id)
                val script = Json.Mapper.convertValue<ZpsScript>(event.payload)
                expand(task, script)
            }
            TaskEventType.MESSAGE -> {
                val message = Json.Mapper.convertValue<TaskMessageEvent>(event.payload)
                logger.warn("Task Event Message: ${task.id} : $message")
            }
        }
    }

    fun killRunningTaskOnAnalyst(task: Task, newState: TaskState, reason: String): Boolean {
        if (task.host == null) {
            logger.warn("Failed to kill running task, no host is set")
            return false
        }
        return analystService.killTask(task.host, task.id, reason, newState)
    }

    override fun retryTask(task: Task): Boolean {
        return if (task.state.isDispatched()) {
            GlobalScope.launch {
                killRunningTaskOnAnalyst(task, TaskState.Waiting, "Task retried by ")
            }
            // just assuming true here as the call to the analyst is backgrounded
            true
        } else {
            jobService.setTaskState(task, TaskState.Waiting, null)
        }
    }

    override fun skipTask(task: Task): Boolean {
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
