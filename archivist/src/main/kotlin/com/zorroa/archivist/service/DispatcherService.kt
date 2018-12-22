package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.*
import com.zorroa.archivist.security.*
import com.zorroa.archivist.util.event
import com.zorroa.common.clients.RestClient
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.annotation.PostConstruct

interface DispatcherService {
    fun getNext() : DispatchTask?
    fun startTask(task: TaskId) : Boolean
    fun stopTask(task: TaskId, exitStatus: Int, overrideState: TaskState?=null) : Boolean
    fun handleEvent(event: TaskEvent)
    fun expand(parentTask: Task, script: ZpsScript) : Task
    fun expand(job: JobId, script: ZpsScript) : Task
    fun retryTask(task: Task): Boolean
    fun skipTask(task: Task): Boolean
    fun queueTask(task: TaskId, endpoint: String) : Boolean
}

@Service
@Transactional
class DispatcherServiceImpl @Autowired constructor(
        private val dispatchTaskDao: DispatchTaskDao,
        private val taskDao: TaskDao,
        private val jobDao: JobDao,
        private val taskErrorDao: TaskErrorDao,
        private val properties: ApplicationProperties,
        private val userDao: UserDao,
        private val analystDao: AnalystDao,
        private val eventBus: EventBus) : DispatcherService {


    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var fileStorageService: FileStorageService

    @PostConstruct
    fun init() {
        // Register for event bus
        eventBus.register(this)
    }

    override fun getNext(): DispatchTask? {
        val endpoint = getAnalystEndpoint()
        if (analystDao.isInLockState(endpoint, LockState.Locked)) {
            return null
        }

        if (endpoint != null ) {
            val tasks = dispatchTaskDao.getNext(5)
            for (task in tasks) {
                if (queueTask(task, endpoint)) {

                    task.env["ZORROA_TASK_ID"] = task.id.toString()
                    task.env["ZORROA_JOB_ID"] = task.jobId.toString()
                    task.env["ZORROA_AUTH_TOKEN"] = generateUserToken(userDao.getApiKey(task.userId))
                    if (properties.getBoolean("archivist.debug-mode.enabled")) {
                        task.env["ZORROA_DEBUG_MODE"] = "true"
                    }
                    withAuth(SuperAdminAuthentication(task.organizationId)) {
                        val fs = fileStorageService.get(task.getLogSpec())
                        task.logFile = fs.getServableFile().getSignedUrl().toString()
                    }
                    // Set the time started on the job if its not set already.
                    jobDao.setTimeStarted(task)

                    return task
                }
            }
        }
        return null
    }

    override fun queueTask(task: TaskId, endpoint: String) : Boolean {
        val result = taskDao.setState(task, TaskState.Queued, TaskState.Waiting)
        return if (result) {
            taskDao.setHostEndpoint(task, endpoint)
            analystDao.setTaskId(endpoint, task.taskId)
            true
        }
        else {
            false
        }
    }

    override fun startTask(task: TaskId) : Boolean {
        val result =  taskDao.setState(task, TaskState.Running, TaskState.Queued)
        logger.info("Starting task: {}, {}", task.taskId, result)
        return result
    }

    override fun stopTask(task: TaskId, exitStatus: Int, overrideState: TaskState?) : Boolean {

        val newState = when {
            overrideState != null -> overrideState
            exitStatus != 0 -> TaskState.Failure
            else -> TaskState.Success
        }

        val stopped = when {
            taskDao.setState(task, newState, TaskState.Running) -> true
            taskDao.setState(task, newState, TaskState.Queued) -> true
            else -> false
        }

        if (stopped) {
            taskDao.setExitStatus(task, exitStatus)
            try {
                val endpoint = getAnalystEndpoint()
                analystDao.setTaskId(endpoint, null)
            }
            catch(e: Exception) {
                logger.warn("Failed to clear taskId from Analyst")
            }
        }

        logger.info("Stopping task: {}, newState={}, result={}", task.taskId, newState, stopped)
        return stopped
    }

    override fun expand(parentTask: Task, script: ZpsScript) : Task {

        val parentScript = taskDao.getScript(parentTask.id)
        script.globals = parentScript.globals
        script.type = parentScript.type
        script.inline = true
        script.settings = parentScript.settings

        if (script.execute.orEmpty().isEmpty()) {
            script.execute = parentScript.execute
        }

        val newTask =  taskDao.create(parentTask, TaskSpec(zpsTaskName(script), script))
        logger.info("Expanding parent task: {} with task: {}", parentTask.id, newTask.id)
        return newTask
    }

    override fun expand(job: JobId, script: ZpsScript) : Task {
        val newTask =  taskDao.create(job, TaskSpec(zpsTaskName(script), script))
        logger.info("Expanding job: {} with task: {}", job.jobId, newTask.id)
        return newTask
    }

    override fun handleEvent(event: TaskEvent) {
        val task = taskDao.get(event.taskId)
        when(event.type) {
            TaskEventType.STOPPED -> {
                val payload = Json.Mapper.convertValue<TaskStoppedEvent>(event.payload)
                stopTask(task, payload.exitStatus, payload.newState)
            }
            TaskEventType.STARTED -> startTask(task)
            TaskEventType.ERROR-> {
                // Might have to queue and submit in batches
                val payload = Json.Mapper.convertValue<TaskErrorEvent>(event.payload)
                taskErrorDao.create(event, payload)
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

    fun killRunningTaskOnAnalyst(task: Task, newState: TaskState, reason: String) : Boolean {
        if (task.host == null) {
            logger.warn("Failed to kill running task, no host is set")
            return false
        }
        try {
            logger.event("Task kill",
                    mapOf("reason" to reason, "taskId" to task.id, "jobId" to task.jobId))
            val client = RestClient(task.host)
            val result = client.delete("/kill/" + task.id,
                    mapOf("reason" to reason + getUsername(), "state" to newState.name), Json.GENERIC_MAP)

            return if (result["status"] as Boolean) {
                true
            }
            else {
                logger.warn("Failed to kill task {} on host {}, result: {}", task.id, task.host, result)
                false
            }

        } catch (e: Exception) {
            logger.warn("Failed to kill running task an analyst {}", task.host, e)
        }
        return false
    }

    override fun retryTask(task: Task): Boolean {
        return if (task.state.isDispatched()) {
            GlobalScope.launch {
                killRunningTaskOnAnalyst(task, TaskState.Waiting, "Task retried by ")
            }
            // just assuming true here as the call to the analyst is backgrounded
            true
        }
        else {
            jobService.setTaskState(task, TaskState.Waiting, null)
        }
    }

    override fun skipTask(task: Task) : Boolean {
        return if (task.state.isDispatched()) {
            GlobalScope.launch {
                killRunningTaskOnAnalyst(task, TaskState.Skipped, "Task skipped by ")
            }
            // just assuming true here as the call to the analyst is backgrounded
            true
        }
        else {
            jobService.setTaskState(task, TaskState.Skipped, null)
        }
    }

    @Subscribe
    fun handleJobStateChangeEvent(event: JobStateChangeEvent) {
        GlobalScope.launch {
            if (event.newState == JobState.Cancelled) {
                handleJobCanceled(event.job)
            }
        }
    }

    fun handleJobCanceled(job: Job) {
        for (task in  taskDao.getAll(job.id, TaskState.Running)) {
            killRunningTaskOnAnalyst(task, TaskState.Waiting, "Job canceled by ")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DispatcherServiceImpl::class.java)
    }
}
