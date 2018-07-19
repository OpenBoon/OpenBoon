package com.zorroa.analyst.service

import com.zorroa.analyst.repository.JobDao
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.JobState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface JobService {
    fun create(spec: JobSpec) : Job
    fun get(id: UUID) : Job
    fun get(name: String) : Job
    fun finish(job: Job) : Boolean
}

@Transactional
@Service
class JobServiceImpl @Autowired constructor(
        val jobDao: JobDao): JobService {

    override fun get(id: UUID) : Job {
        return jobDao.get(id)
    }

    override fun get(name: String) : Job {
        return jobDao.get(name)
    }

    override fun create(spec: JobSpec) : Job {
        return jobDao.create(spec)
    }

    override fun finish(job: Job) : Boolean {
        val result = jobDao.setState(job, JobState.SUCCESS, JobState.RUNNING)
        logger.info("Job {} RUNNING->SUCCESS: {}", job.name, result)
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobServiceImpl::class.java)
    }
}
