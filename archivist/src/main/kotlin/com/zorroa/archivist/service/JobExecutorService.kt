package com.zorroa.archivist.service

import com.google.common.cache.CacheBuilder
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.google.common.util.concurrent.AbstractScheduledService
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.archivist.security.getUsername
import com.zorroa.cluster.client.WorkerNodeClient
import com.zorroa.cluster.thrift.TaskKillT
import com.zorroa.cluster.thrift.TaskResultT
import com.zorroa.cluster.thrift.TaskStartT
import com.zorroa.common.domain.AnalystState
import com.zorroa.common.domain.TaskState
import com.zorroa.sdk.client.exception.ArchivistWriteException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

/**
 * Created by chambers on 6/24/16.
 */
interface JobExecutorService {

    fun queueWaitingTasks(url: String, count: Int): Future<List<TaskStartT>>

    fun getWaitingTasks(url: String?, count: Int): List<TaskStartT>

    fun handleResponse(task: Task, response: TaskResultT)

    @Throws(InterruptedException::class)
    fun waitOnResponse(job: Job): Any

    @Async
    fun retryTask(task: Task)

    @Async
    fun retryAllFailures(job: JobId)

    @Async
    fun skipTask(task: Task)

    fun cancelJob(job: JobId): Boolean

    fun restartJob(job: JobId): Boolean
}

@Component
class JobExecutorServiceImpl @Autowired constructor(
        val analystDao: AnalystDao,
        val taskDao: TaskDao,
        val properties: ApplicationProperties
): AbstractScheduledService(), JobExecutorService, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private lateinit var jobService: JobService

    private val commandQueue = Executors.newSingleThreadExecutor()

    private val returnQueue = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build<Int, SynchronousQueue<Any>>()

    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent) {
        if (!ArchivistConfiguration.unittest) {
            startAsync()
        }
    }

    @Throws(Exception::class)
    override fun runOneIteration() {
        /**
         * Note that, if this function throws then scheduling will stop
         * so just in case we're not letting anything bubble up from here.
         */
        try {
            checkForUnresponsiveAnalysts()
            checkForExpiredTasks()
        } catch (e: Exception) {
            logger.warn("Job executor failed to schedule tasks, ", e)
        }

    }

    override fun queueWaitingTasks(url: String, count: Int): Future<List<TaskStartT>> {
        return commandQueue.submit<List<TaskStartT>> { getWaitingTasks(url, count) }
    }

    override fun getWaitingTasks(url: String?, count: Int): List<TaskStartT> {
        if (url == null) {
            throw ArchivistWriteException("Failed to query for tasks, return URL is null. " + "Analyst may be badly configured")
        }
        if (count < 1) {
            return ImmutableList.of()
        }

        val result = Lists.newArrayListWithCapacity<TaskStartT>(count)
        for (task in taskDao.getWaiting(count)) {
            if (jobService.setTaskQueued(TaskIdImpl(task), url)) {
                result.add(task)
            }
        }
        if (!result.isEmpty()) {
            val taskIds = result
                    .stream()
                    .map { s -> s.getId().toString() }
                    .collect(Collectors.joining(","))
            logger.info("{} asking for {} tasks, returned {}", url, taskIds, result.size)
        }
        return result
    }

    override fun handleResponse(task: Task, response: TaskResultT) {
        logger.info("Processing job response, id:{}", task.jobId)
        try {
            val queue = returnQueue.asMap()[task.jobId]
            if (queue == null) {
                logger.warn("Synchronous queue expired for job: {}", task.jobId)
                return
            }
            queue.offer(response.getResult(), 30, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            logger.warn("Waiting thread disappeared for job response ID: {}",
                    task.jobId, e)
        }

    }

    override fun waitOnResponse(job: Job): Any {
        returnQueue.put(job.id, SynchronousQueue())
        try {
            return returnQueue.asMap()[job.id]!!.poll(30, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            throw IllegalStateException("Failed waiting on response, ", e)
        }
    }

    /**
     * Look for analysts that we have not heard from in some time
     *
     * TODO: may need to verify with analyst that its still around.
     */
    fun checkForUnresponsiveAnalysts() {
        val timeout = properties.getInt("archivist.maintenance.analyst.inactiveTimeoutSeconds") * 1000L
        for (a in analystDao.getUnresponsive(25, timeout)) {
            logger.warn("Setting analyst {}/{} to DOWN state", a.url, a.id)
            analystDao.setState(a.id, AnalystState.DOWN)
        }
    }

    /**
     * Look for tasks that have been queued or running without a ping from an analyst.
     *
     * TODO: may need to verify with analyst that its still around.
     */
    fun checkForExpiredTasks() {
        val expired = taskDao.getOrphanTasks(10, 10, TimeUnit.MINUTES)
        if (!expired.isEmpty()) {
            logger.warn("Found {} expired tasks!", expired.size)
            for (task in expired) {
                logger.warn("resetting task {} to Waiting", task.taskId)
                jobService.setTaskState(task, TaskState.Waiting)
                /**
                 * TODO: contact analyst and kill
                 */
            }
        }
    }

    @Async
    override fun retryTask(task: Task) {
        if (TaskState.requiresStop(task.state)) {
            killRunningTaskOnAnalyst(task, TaskState.Waiting)
        } else {
            if (jobService.setTaskState(task, TaskState.Waiting)) {
                jobService.decrementStats(task)
            }
        }
    }

    @Async
    override fun retryAllFailures(job: JobId) {
        for (task in jobService.getTasks(job.jobId, TaskState.Failure)) {
            if (jobService.setTaskState(task, TaskState.Waiting)) {
                jobService.decrementStats(task)
            }
        }
    }

    @Async
    override fun skipTask(task: Task) {
        if (TaskState.requiresStop(task.state)) {
            killRunningTaskOnAnalyst(task, TaskState.Skipped)
        } else {
            jobService.setTaskState(task, TaskState.Skipped)
        }
    }

    override fun cancelJob(job: JobId): Boolean {
        val result = jobService.setJobState(job, JobState.Cancelled, JobState.Active)

        for (task in taskDao.getAll(job.jobId,
                TaskFilter().setStates(ImmutableSet.of(TaskState.Queued, TaskState.Running)))) {
            retryTask(task)
        }
        return result
    }

    override fun restartJob(job: JobId): Boolean {
        return jobService.setJobState(job, JobState.Active, JobState.Cancelled)
    }

    fun killRunningTaskOnAnalyst(task: Task, newState: TaskState) {
        val client = WorkerNodeClient(task.host)
        try {
            logger.info("Killing runinng task: {}", task)
            client.killTask(TaskKillT().setId(task.taskId)
                    .setReason("Manually killed  by " + getUsername())
                    .setUser(getUsername()))
        } catch (e: Exception) {
            logger.warn("Failed to kill running task an analyst {}", task.host, e)
        }

    }

    override fun scheduler(): AbstractScheduledService.Scheduler {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(10, 2, TimeUnit.SECONDS)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(JobExecutorServiceImpl::class.java)
    }
}
