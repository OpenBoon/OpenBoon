package com.zorroa.archivist.service

import com.zorroa.archivist.config.NetworkEnvironment
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.AssetIndexResult
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.util.event
import com.zorroa.common.domain.*
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
    fun incrementAssetCounts(task: Task,  counts: AssetIndexResult)
}

@Service
@Transactional
class JobServiceImpl @Autowired constructor(
        private val networkEnvironment: NetworkEnvironment,
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
                    execute.add(ProcessorRef("zplugins.core.collector.ExportCollector"))
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

    override fun createTask(job: JobId, spec: TaskSpec) : Task {
        return taskDao.create(job, spec)
    }

    override fun incrementAssetCounts(task: Task,  counts: AssetIndexResult) {
        taskDao.incrementAssetStats(task, counts)
        jobDao.incrementAssetStats(task, counts)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobServiceImpl::class.java)
    }
}
