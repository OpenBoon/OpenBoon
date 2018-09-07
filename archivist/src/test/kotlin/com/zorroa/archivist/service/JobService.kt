package com.zorroa.archivist.service

import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.common.domain.*
import com.zorroa.common.server.NetworkEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface JobService {
    fun create(spec: JobSpec) : Job
    fun get(id: UUID) : Job
    fun getTask(id: UUID) : Task
}

@Service
@Transactional
class JobServiceImpl @Autowired constructor(
        private val networkEnvironment: NetworkEnvironment,
        private val jobDao: JobDao,
        private val taskDao: TaskDao
): JobService {

    override fun create(spec: JobSpec) : Job {
        /*
        spec.env.putAll(mapOf(
                "ZORROA_SUPER_ADMIN" to "true",
                "ZORROA_ARCHIVIST_URL" to networkEnvironment.getPublicUrl("zorroa-archivist"),
                "ZORROA_ORGANIZATION_ID" to getOrgId().toString()))
        */
        val job = jobDao.create(spec)

        for (script in spec.scripts) {
            taskDao.create(job, TaskSpec(script.name, script))
        }
        return job
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID) : Job {
        return jobDao.get(id)
    }

    @Transactional(readOnly = true)
    override fun getTask(id: UUID) : Task {
        return taskDao.get(id)
    }
}
