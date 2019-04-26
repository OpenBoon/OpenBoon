package com.zorroa.archivist.service

import com.google.common.eventbus.EventBus
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.service.MeterRegistryHolder.getTags
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPagedList
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

interface JobService {
    fun create(spec: JobSpec) : Job
    fun create(spec: JobSpec, type: PipelineType) : Job
    fun get(id: UUID, forClient:Boolean=false) : Job
    fun getTask(id: UUID) : Task
    fun createTask(job: JobId, spec: TaskSpec) : Task
    fun getAll(filter: JobFilter?): KPagedList<Job>
    fun incrementAssetCounts(task: Task,  counts: BatchCreateAssetsResponse)
    fun setJobState(job: JobId, newState: JobState, oldState: JobState?): Boolean
    fun setTaskState(task: Task, newState: TaskState, oldState: TaskState?): Boolean
    fun cancelJob(job: Job) : Boolean
    fun restartJob(job: Job) : Boolean
    fun retryAllTaskFailures(job: JobId) : Int
    fun getZpsScript(id: UUID) : ZpsScript
    fun updateJob(job: Job, spec: JobUpdateSpec) : Boolean
    fun getTaskErrors(filter: TaskErrorFilter) : KPagedList<TaskError>
    fun deleteTaskError(id: UUID): Boolean
    fun getTaskLog(id: UUID) : ServableFile
    fun deleteJob(job: JobId) : Boolean
    fun getExpiredJobs(duration: Long, unit: TimeUnit, limit: Int) : List<Job>
    fun checkAndSetJobFinished(job: JobId): Boolean
    fun getOrphanTasks(duration: Duration) : List<Task>
}

@Service
@Transactional
class JobServiceImpl @Autowired constructor(
        val eventBus: EventBus,
        val jobDao: JobDao,
        val taskDao: TaskDao,
        val taskErrorDao: TaskErrorDao,
        val txevent: TransactionEventManager

): JobService {

    @Autowired
    private lateinit var pipelineService: PipelineService

    @Autowired
    lateinit var fileStorageService: FileStorageService

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
        if (spec.replace) {
            /**
             * If old job is being replaced, then add a commit hook to kill
             * the old job.
             */
            txevent.afterCommit(sync=false) {
                val filter = JobFilter(
                        states=listOf(JobState.Active),
                        names=listOf(job.name),
                        organizationIds=listOf(getOrgId()))
                val oldJobs = jobDao.getAll(filter)
                for (oldJob in oldJobs) {
                    // Don't kill one we just made
                    if (oldJob.id != job.id) {
                        logger.event(LogObject.JOB, LogAction.REPLACE,
                                mapOf("jobId" to oldJob.id, "jobName" to oldJob.name))
                        cancelJob(oldJob)
                    }
                }
            }
        }

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
                    execute.add(ProcessorRef("zplugins.core.collectors.ImportCollector"))
                }
                PipelineType.Export-> {
                    script.setSettting("inline", true)
                    script.setGlobalArg("exportArgs", mapOf(
                            "exportId" to job.id,
                            "exportName" to job.name))
                    execute.add(ProcessorRef("zplugins.core.collectors.ExportCollector"))
                }
                PipelineType.Batch,PipelineType.Generate-> { }

            }
            script.execute = execute
            taskDao.create(job, TaskSpec(zpsTaskName(script), script))
        }

        return get(job.id)
    }

    override fun updateJob(job: Job, spec: JobUpdateSpec) : Boolean {
        return jobDao.update(job, spec)
    }

    override fun deleteJob(job: JobId) : Boolean {
        return jobDao.delete(job)
    }

    @Transactional(readOnly = true)
    override fun getExpiredJobs(duration: Long, unit: TimeUnit, limit: Int) : List<Job> {
        return jobDao.getExpired(duration, unit, limit)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID, forClient:Boolean) : Job {
        return jobDao.get(id, forClient)
    }

    @Transactional(readOnly = true)
    override fun getAll(filter: JobFilter?): KPagedList<Job> {
        return jobDao.getAll(filter)
    }

    @Transactional(readOnly = true)
    override fun getTask(id: UUID) : Task {
        return taskDao.get(id)
    }

    @Transactional(readOnly = true)
    override fun getOrphanTasks(duration: Duration) : List<Task> {
        return taskDao.getOrphans(duration)
    }


    @Transactional(readOnly = true)
    override fun getZpsScript(id: UUID) : ZpsScript {
        return taskDao.getScript(id)
    }

    @Transactional(readOnly = true)
    override fun getTaskLog(id: UUID) : ServableFile {
        val task = getTask(id)
        val st = fileStorageService.get(task.getLogSpec())
        return st.getServableFile()
    }

    override fun createTask(job: JobId, spec: TaskSpec) : Task {
        return taskDao.create(job, spec)
    }

    override fun incrementAssetCounts(task: Task, counts: BatchCreateAssetsResponse) {
        taskDao.incrementAssetStats(task, counts)
        jobDao.incrementAssetStats(task, counts)
    }

    override fun setJobState(job: JobId, newState: JobState, oldState: JobState?): Boolean  {
        val result = jobDao.setState(job, newState, oldState)
        if (result) {
            eventBus.post(JobStateChangeEvent(get(job.jobId), newState, oldState))
        }
        return result
    }

    override fun setTaskState(task: Task, newState: TaskState, oldState: TaskState?): Boolean  {
        val result =  taskDao.setState(task, newState, oldState)
        if (result) {
            if (newState == TaskState.Success || newState == TaskState.Skipped) {
                checkAndSetJobFinished(task)
            }
            eventBus.post(TaskStateChangeEvent(task, newState, oldState))
        }
        return result
    }

    override fun cancelJob(job: Job) : Boolean {
        return setJobState(job, JobState.Cancelled, JobState.Active)
    }

    override fun restartJob(job: Job) : Boolean {
        return setJobState(job, JobState.Active, null)
    }

    override fun retryAllTaskFailures(job: JobId) : Int {
        var count = 0
        for (task in taskDao.getAll(job.jobId, TaskState.Failure)) {
            if (setTaskState(task, TaskState.Waiting, TaskState.Failure)) {
                count++
            }
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun getTaskErrors(filter: TaskErrorFilter): KPagedList<TaskError> {
        return taskErrorDao.getAll(filter)
    }

    override fun deleteTaskError(id: UUID): Boolean {
        return taskErrorDao.delete(id)
    }

    override fun checkAndSetJobFinished(job: JobId): Boolean  {
        if (!jobDao.hasPendingFrames(job)) {
            return setJobState(job, JobState.Finished, JobState.Active)
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobServiceImpl::class.java)
    }
}
