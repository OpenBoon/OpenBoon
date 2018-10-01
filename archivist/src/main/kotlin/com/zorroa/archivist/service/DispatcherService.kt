package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.*
import com.zorroa.archivist.security.generateUserToken
import com.zorroa.archivist.security.getAnalystEndpoint
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface DispatcherService {
    fun getNext() : DispatchTask?
    fun startTask(task: TaskId) : Boolean
    fun stopTask(task: TaskId, exitStatus: Int) : Boolean
    fun handleEvent(event: TaskEvent)
    fun expand(parentTask: Task, script: ZpsScript) : Task
    fun expand(job: JobId, script: ZpsScript) : Task
}

@Service
@Transactional
class DispatcherServiceImpl @Autowired constructor(
        private val dispatchTaskDao: DispatchTaskDao,
        private val taskDao: TaskDao,
        private val taskErrorDao: TaskErrorDao,
        private val jobDao: JobDao,
        private val userDao: UserDao) : DispatcherService {

    @Autowired
    lateinit var jobService: JobService


    override fun getNext(): DispatchTask? {
        val endpoint = getAnalystEndpoint()
        if (endpoint != null ) {
            val tasks = dispatchTaskDao.getNext(5)
            for (task in tasks) {
                if (taskDao.setState(task, TaskState.Queued, TaskState.Waiting)) {
                    taskDao.setHostEndpoint(task, endpoint)
                    task.env["ZORROA_TASK_ID"] = task.id.toString()
                    task.env["ZORROA_JOB_ID"] = task.jobId.toString()
                    task.env["ZORROA_AUTH_TOKEN"] = generateUserToken(userDao.getApiKey(task.userId))
                    return task
                }
            }
        }
        return null
    }

    override fun startTask(task: TaskId) : Boolean {
        val result =  taskDao.setState(task, TaskState.Running, TaskState.Queued)
        logger.info("Starting task: {}, {}", task.taskId, result)
        return result
    }

    override fun stopTask(task: TaskId, exitStatus: Int) : Boolean {

        val newState = if (exitStatus != 0) {
            TaskState.Failure
        }
        else {
            TaskState.Success
        }
        val result =  if (taskDao.setState(task, newState, TaskState.Running)) {
            taskDao.setExitStatus(task, exitStatus)
            true
        }
        else {
            false
        }

        logger.info("Stopping task: {}, newState={}, result={}", task.taskId, newState, result)
        return result
    }

    override fun expand(parentTask: Task, script: ZpsScript) : Task {

        val parentScript = taskDao.getScript(parentTask.id)
        script.globals = parentScript.globals
        script.type = parentScript.type
        script.inline = true

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
                stopTask(task, payload.exitStatus)
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

    companion object {
        private val logger = LoggerFactory.getLogger(DispatcherServiceImpl::class.java)
    }
}
