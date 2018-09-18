package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.DispatchTaskDao
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface DispatcherService {
    fun getNext(host: String) : DispatchTask?
    fun startTask(task: TaskId) : Boolean
    fun stopTask(task: TaskId, exitStatus: Int) : Boolean
    fun handleEvent(event: TaskEvent)
    fun expand(job: Job, script: ZpsScript) : Task
}

@Service
@Transactional
class DispatcherServiceImpl @Autowired constructor(
        private val dispatchTaskDao: DispatchTaskDao,
        private val taskDao: TaskDao,
        private val taskErrorDao: TaskErrorDao,
        private val jobDao: JobDao) : DispatcherService {

    @Autowired
    lateinit var jobService: JobService

    override fun getNext(host: String): DispatchTask? {
        val tasks = dispatchTaskDao.getNext(5)
        for (task in tasks) {
            if (taskDao.setState(task, TaskState.Queued, TaskState.Waiting)) {
                taskDao.setHostEndpoint(task, host)
                return task
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

    override fun expand(job: Job, script: ZpsScript) : Task {
        val task =  taskDao.create(job, TaskSpec(zpsTaskName(script), script))
        logger.info("Expanding job: {} with task: {}", job.id, task.id)
        return task
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
                val job = jobDao.get(task.jobId)
                val script = Json.Mapper.convertValue<ZpsScript>(event.payload)
                expand(job, script)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DispatcherServiceImpl::class.java)
    }
}
