package com.zorroa.archivist.service

import com.zorroa.archivist.config.NetworkEnvironment
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.common.domain.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface JobService {
    fun create(spec: JobSpec) : Job
    fun get(id: UUID) : Job
    fun getTask(id: UUID) : Task
    fun createTask(job: JobId, spec: TaskSpec) : Task
    fun getAll(page: Pager, filter: JobFilter): PagedList<Job>
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
        val job = jobDao.create(spec)

        for (script in spec.scripts) {
            if (script.execute == null) {
                script.execute = pipelineService.resolveDefault(spec.type)
            }
            else {
                script.execute = pipelineService.resolve(spec.type, script.execute)
            }
            taskDao.create(job, TaskSpec(script.name, script))
        }
        return job
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

}
