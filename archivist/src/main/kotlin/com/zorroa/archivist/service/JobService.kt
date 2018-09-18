package com.zorroa.archivist.service

import com.zorroa.archivist.config.NetworkEnvironment
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.zpsTaskName
import com.zorroa.archivist.repository.AssetIndexResult
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.common.domain.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface JobService {
    fun create(spec: JobSpec) : Job
    fun create(spec: JobSpec, type: PipelineType) : Job
    fun get(id: UUID) : Job
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
        val job = jobDao.create(spec, type)

        spec.script?.let { script->
            if (script.execute == null) {
                script.execute = pipelineService.resolveDefault(job.type)
            }
            else {
                script.execute = pipelineService.resolve(job.type, script.execute)
            }
            taskDao.create(job, TaskSpec(zpsTaskName(script), script))
        }
        return get(job.id)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID) : Job {
        return jobDao.get(id)
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

}
