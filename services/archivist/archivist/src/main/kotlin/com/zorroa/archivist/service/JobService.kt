package com.zorroa.archivist.service

import com.google.common.eventbus.EventBus
import com.zorroa.archivist.domain.AssetCounters
import com.zorroa.archivist.domain.InternalTask
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobFilter
import com.zorroa.archivist.domain.JobId
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.JobStateChangeEvent
import com.zorroa.archivist.domain.JobUpdateSpec
import com.zorroa.archivist.domain.ProjectDirLocator
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.TaskError
import com.zorroa.archivist.domain.TaskErrorFilter
import com.zorroa.archivist.domain.TaskFilter
import com.zorroa.archivist.domain.TaskSpec
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.domain.TaskStateChangeEvent
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.domain.zpsTaskName
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.archivist.util.isUUID
import com.zorroa.zmlp.service.logging.event
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

interface JobService {
    fun create(spec: JobSpec): Job
    fun get(id: UUID, forClient: Boolean = false): Job
    fun getTask(id: UUID): Task
    fun getTasks(jobId: UUID): KPagedList<Task>
    fun getInternalTask(id: UUID): InternalTask
    fun createTask(job: JobId, spec: ZpsScript): Task
    fun getAll(filter: JobFilter?): KPagedList<Job>
    fun incrementAssetCounters(task: InternalTask, counts: AssetCounters)
    fun setJobState(job: JobId, newState: JobState, oldState: JobState?): Boolean
    fun setTaskState(task: InternalTask, newState: TaskState, oldState: TaskState?): Boolean
    fun cancelJob(job: Job): Boolean
    fun restartJob(job: JobId): Boolean
    fun retryAllTaskFailures(job: JobId): Int
    fun getZpsScript(id: UUID): ZpsScript
    fun updateJob(job: Job, spec: JobUpdateSpec): Boolean
    fun getTaskErrors(filter: TaskErrorFilter): KPagedList<TaskError>
    fun deleteTaskError(id: UUID): Boolean
    fun deleteJob(job: JobId): Boolean
    fun getExpiredJobs(duration: Long, unit: TimeUnit, limit: Int): List<Job>
    fun checkAndSetJobFinished(job: JobId): Boolean
    fun getOrphanTasks(duration: Duration): List<InternalTask>
    fun findOneJob(filter: JobFilter): Job
    fun findOneTaskError(filter: TaskErrorFilter): TaskError
    fun setCredentials(job: JobId, names: Collection<String>)
    fun getCredentialsTypes(job: JobId): List<String>
}

@Service
@Transactional
class JobServiceImpl @Autowired constructor(
    val eventBus: EventBus,
    val jobDao: JobDao,
    val taskDao: TaskDao,
    val taskErrorDao: TaskErrorDao,
    val txevent: TransactionEventManager,
    val projectStorageService: ProjectStorageService
) : JobService {

    @Autowired
    private lateinit var pipelineResolverService: PipelineResolverService

    @Autowired
    private lateinit var credentialsService: CredentialsService

    @Autowired
    private lateinit var dependService: DependService

    override fun create(spec: JobSpec): Job {
        val user = getZmlpActor()
        if (spec.name == null) {
            val date = Date()
            spec.name = "Job launched by ${user.projectId} on $date"
        }

        val job = jobDao.create(spec)

        if (spec.replace) {
            /**
             * If old job is being replaced, then add a commit hook to kill
             * the old job.
             */
            txevent.afterCommit(sync = false) {
                val filter = JobFilter(
                    states = listOf(JobState.InProgress),
                    names = listOf(job.name)
                )
                val oldJobs = jobDao.getAll(filter)
                for (oldJob in oldJobs) {
                    // Don't kill one we just made
                    if (oldJob.id != job.id) {
                        logger.event(
                            LogObject.JOB, LogAction.REPLACE,
                            mapOf("jobId" to oldJob.id, "jobName" to oldJob.name)
                        )
                        cancelJob(oldJob)
                    }
                }
            }
        }

        spec.scripts?.forEach {
            createTask(job, it)
        }

        spec.dependOnJobIds?.let {
            // Might have to check status of job.
            dependService.createDepend(job, it)
        }

        logger.event(
            LogObject.JOB, LogAction.CREATE,
            mapOf(
                "jobId" to job.id,
                "jobName" to job.name
            )
        )

        spec.credentials?.let {
            setCredentials(job, it)
        }

        return get(job.id)
    }

    override fun createTask(job: JobId, script: ZpsScript): Task {
        script.execute = pipelineResolverService.resolveCustom(script.execute).execute
        val task = taskDao.create(job, TaskSpec(zpsTaskName(script), script))

        incrementAssetCounters(
            task,
            AssetCounters(script.assetIds?.size ?: 0)
        )

        logger.event(
            LogObject.TASK, LogAction.CREATE,
            mapOf(
                "taskId" to task.id,
                "taskName" to task.name
            )
        )

        script.children?.forEach {
            val childTask = createTask(job, it)
            dependService.createDepend(task, listOf(childTask))
        }
        return task
    }

    override fun updateJob(job: Job, spec: JobUpdateSpec): Boolean {

        logger.event(
            LogObject.JOB, LogAction.UPDATE,
            mapOf(
                "jobId" to job.id,
                "jobName" to job.name
            )
        )

        return jobDao.update(job, spec)
    }

    override fun deleteJob(job: JobId): Boolean {

        logger.event(
            LogObject.JOB, LogAction.DELETE,
            mapOf(
                "jobId" to job.jobId
            )
        )

         projectStorageService.recursiveDelete(
             ProjectDirLocator(
                 ProjectStorageEntity.JOB,
                 job.jobId.toString(),
                 getProjectId()
             )
         )

        return jobDao.delete(job)
    }

    @Transactional(readOnly = true)
    override fun getExpiredJobs(duration: Long, unit: TimeUnit, limit: Int): List<Job> {
        return jobDao.getExpired(duration, unit, limit)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID, forClient: Boolean): Job {
        return jobDao.get(id, forClient)
    }

    @Transactional(readOnly = true)
    override fun getAll(filter: JobFilter?): KPagedList<Job> {
        return jobDao.getAll(filter)
    }

    override fun findOneJob(filter: JobFilter): Job {
        return jobDao.findOneJob(filter)
    }

    @Transactional(readOnly = true)
    override fun getTask(id: UUID): Task {
        return taskDao.get(id)
    }

    @Transactional(readOnly = true)
    override fun getTasks(jobId: UUID): KPagedList<Task> {
        val filter = TaskFilter(jobIds = arrayListOf(jobId))
        return taskDao.getAll(filter)
    }

    @Transactional(readOnly = true)
    override fun getInternalTask(id: UUID): InternalTask {
        return taskDao.getInternal(id)
    }

    @Transactional(readOnly = true)
    override fun getOrphanTasks(duration: Duration): List<InternalTask> {
        return taskDao.getOrphans(duration)
    }

    @Transactional(readOnly = true)
    override fun getZpsScript(id: UUID): ZpsScript {
        return taskDao.getScript(id)
    }

    override fun incrementAssetCounters(task: InternalTask, counts: AssetCounters) {
        taskDao.incrementAssetCounters(task, counts)
        jobDao.incrementAssetCounters(task, counts)
    }

    override fun setJobState(job: JobId, newState: JobState, oldState: JobState?): Boolean {

        val result = jobDao.setState(job, newState, oldState)
        if (result) {
            eventBus.post(JobStateChangeEvent(get(job.jobId), newState, oldState))
        }

        logger.event(
            LogObject.JOB, LogAction.UPDATE,
            mapOf(
                "jobId" to job.jobId,
                "jobState" to newState.name
            )
        )

        return result
    }

    override fun setTaskState(task: InternalTask, newState: TaskState, oldState: TaskState?): Boolean {
        val result = taskDao.setState(task, newState, oldState)
        if (result) {
            /**
             * If the task finished, resolve depends first.
             * Then check to see if job is finished, otherwise
             * the job will finish with waiting frames.
             */
            if (newState.isSuccessState()) {
                dependService.resolveDependsOnTask(task)
            }
            if (newState.isFinishedState()) {
                checkAndSetJobFinished(task)
            }
            eventBus.post(TaskStateChangeEvent(task, newState, oldState))
        }

        logger.event(
            LogObject.TASK, LogAction.UPDATE,
            mapOf(
                "taskId" to task.taskId,
                "taskState" to newState.name
            )
        )

        return result
    }

    override fun cancelJob(job: Job): Boolean {
        return setJobState(job, JobState.Cancelled, JobState.InProgress)
    }

    override fun restartJob(job: JobId): Boolean {
        return setJobState(job, JobState.InProgress, null)
    }

    override fun retryAllTaskFailures(job: JobId): Int {
        var count = 0
        for (task in taskDao.getAll(job.jobId, TaskState.Failure)) {
            // Use DAO here to avoid extra overhead of setTaskState service method
            if (taskDao.setState(task, TaskState.Waiting, TaskState.Failure)) {
                count++
            }
        }
        if (count > 0) {
            restartJob(job)
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun getTaskErrors(filter: TaskErrorFilter): KPagedList<TaskError> {
        return taskErrorDao.getAll(filter)
    }

    @Transactional(readOnly = true)
    override fun findOneTaskError(filter: TaskErrorFilter): TaskError {
        return taskErrorDao.findOneTaskError(filter)
    }

    override fun deleteTaskError(id: UUID): Boolean {

        logger.event(
            LogObject.TASK_ERROR, LogAction.DELETE,
            mapOf(
                "taskId" to id
            )
        )

        return taskErrorDao.delete(id)
    }

    override fun checkAndSetJobFinished(job: JobId): Boolean {
        val counts = jobDao.getTaskStateCounts(job)
        if (!counts.hasPendingTasks()) {
            val newState = if (counts.hasFailures()) {
                JobState.Failure
            } else {
                JobState.Success
            }
            if (setJobState(job, newState, JobState.InProgress)) {
                dependService.resolveDependsOnJob(job)
                return true
            }
        }
        return false
    }

    override fun setCredentials(job: JobId, names: Collection<String>) {
        val creds = names.map {
            if (it.isUUID()) {
                credentialsService.get(UUID.fromString(it))
            } else {
                credentialsService.get(it)
            }
        }
        jobDao.setCredentials(job, creds)
    }

    @Transactional(readOnly = true)
    override fun getCredentialsTypes(job: JobId): List<String> {
        return jobDao.getCredentialsTypes(job)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobServiceImpl::class.java)
    }
}
