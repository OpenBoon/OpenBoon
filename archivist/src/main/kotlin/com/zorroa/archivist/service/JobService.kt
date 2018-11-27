package com.zorroa.archivist.service

import com.google.common.eventbus.EventBus
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.util.event
import com.zorroa.common.domain.*
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface JobService {
    fun create(spec: JobSpec) : Job
    fun create(spec: JobSpec, type: PipelineType) : Job
    fun get(id: UUID, forClient:Boolean=false) : Job
    fun getTask(id: UUID) : Task
    fun createTask(job: JobId, spec: TaskSpec) : Task
    fun getAll(page: Pager, filter: JobFilter): PagedList<Job>
    fun incrementAssetCounts(task: Task,  counts: BatchCreateAssetsResponse)
    fun setJobState(job: Job, newState: JobState, oldState: JobState?): Boolean
    fun setTaskState(task: Task, newState: TaskState, oldState: TaskState?): Boolean
    fun cancelJob(job: Job) : Boolean
    fun restartCanceledJob(job: Job) : Boolean
    fun retryAllTaskFailures(job: JobId) : Int
    fun getZpsScript(id: UUID) : ZpsScript
    fun updateJob(job: Job, spec: JobUpdateSpec) : Boolean
}

@Service
@Transactional
class JobServiceImpl @Autowired constructor(
        private val eventBus: EventBus,
        private val jobDao: JobDao,
        private val taskDao: TaskDao
): JobService {

    @Autowired
    private lateinit var pipelineService: PipelineService

    override fun create(spec: JobSpec) : Job {
        if (spec.script != null) {
            val type = if (spec.script?.type == null) {
                PipelineType.Import
            }
            else {
                spec.script!!.type
            }
            return create(spec, type)
        }
        else {
            throw IllegalArgumentException("Cannot launch job without script to determine type")
        }
    }

    override fun create(spec: JobSpec, type: PipelineType) : Job {
        val user = getUser()
        if (spec.name == null) {
            val date = Date()
            spec.name = "${type.name} job launched by ${user.getName()} on $date"
        }

        val job = jobDao.create(spec, type)

        spec.script?.let { script->

            // Gather up all the procs for execute.
            val execute = if (script.execute == null) {
                pipelineService.resolveDefault(job.type).toMutableList()
            }
            else {
                pipelineService.resolve(job.type, script.execute).toMutableList()
            }

            when(type) {
                PipelineType.Import-> {
                    execute.add(ProcessorRef("zplugins.core.collector.ImportCollector"))
                }
                PipelineType.Export->{
                    script.inline = true
                    execute.add(ProcessorRef("zplugins.export.collectors.ExportCollector"))
                }
                PipelineType.Batch,PipelineType.Generate-> { }

            }
            script.execute = execute
            taskDao.create(job, TaskSpec(zpsTaskName(script), script))
        }

        logger.event("launched Job",
                mapOf("jobName" to job.name, "jobId" to job.id))

        return get(job.id)
    }

    override fun updateJob(job: Job, spec: JobUpdateSpec) : Boolean {
        return jobDao.update(job, spec)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID, forClient:Boolean) : Job {
        return jobDao.get(id, forClient)
    }

    override fun getAll(page: Pager, filter: JobFilter): PagedList<Job> {
        return jobDao.getAll(page, filter)
    }

    @Transactional(readOnly = true)
    override fun getTask(id: UUID) : Task {
        return taskDao.get(id)
    }

    @Transactional(readOnly = true)
    override fun getZpsScript(id: UUID) : ZpsScript {
        return taskDao.getScript(id)
    }

    override fun createTask(job: JobId, spec: TaskSpec) : Task {
        return taskDao.create(job, spec)
    }

    override fun incrementAssetCounts(task: Task,  counts: BatchCreateAssetsResponse) {
        taskDao.incrementAssetStats(task, counts)
        jobDao.incrementAssetStats(task, counts)
    }

    override fun setJobState(job: Job, newState: JobState, oldState: JobState?): Boolean  {
        val result = jobDao.setState(job, newState, oldState)
        if (result) {
            eventBus.post(JobStateChangeEvent(job, newState, oldState))
        }
        return result
    }

    override fun setTaskState(task: Task, newState: TaskState, oldState: TaskState?): Boolean  {
        val result =  taskDao.setState(task, newState, oldState)
        if (result) {
            eventBus.post(TaskStateChangeEvent(task, newState, oldState))
        }
        return result
    }

    override fun cancelJob(job: Job) : Boolean {
        return setJobState(job, JobState.Cancelled, JobState.Active)
    }

    override fun restartCanceledJob(job: Job) : Boolean {
        return setJobState(job, JobState.Active, JobState.Cancelled)
    }

    override fun retryAllTaskFailures(job: JobId) : Int {
        logger.event("Job retryAllTaskFailures", mapOf("jobId" to job.jobId))
        var count = 0
        for (task in taskDao.getAll(job.jobId, TaskState.Failure)) {
            if (setTaskState(task, TaskState.Waiting, TaskState.Failure)) {
                count++
            }
        }
        return count
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobServiceImpl::class.java)
    }
}
