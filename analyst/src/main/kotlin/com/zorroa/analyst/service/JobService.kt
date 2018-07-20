package com.zorroa.analyst.service

import com.zorroa.analyst.domain.LockSpec
import com.zorroa.analyst.repository.JobDao
import com.zorroa.analyst.repository.LockDao
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
    fun stop(job: Job, finalState: JobState) : Boolean
    fun start(job: Job) : Boolean
    fun getWaiting(limit: Int) : List<Job>
    fun getRunning() : List<Job>
    fun setState(job: Job, newState: JobState, oldState: JobState) : Boolean
}

@Transactional
@Service
class JobServiceImpl @Autowired constructor(
        val jobDao: JobDao,
        val lockDao: LockDao): JobService {

    override fun get(id: UUID) : Job {
        return jobDao.get(id)
    }

    override fun get(name: String) : Job {
        return jobDao.get(name)
    }

    override fun create(spec: JobSpec) : Job {
        return jobDao.create(spec)
    }

    override fun setState(job: Job, newState: JobState, oldState: JobState) : Boolean {
        val result = jobDao.setState(job, newState, oldState)
        if (result) {
            logger.info("SUCCESS JOB State Change: {} {}->{}",
                    job.name, oldState.name, newState.name)
        }
        else {
            logger.warn("FAILED JOB State Change: {} {}->{}",
                    job.name, oldState.name, newState.name)
        }
        return result
    }

    override fun start(job: Job) : Boolean {
        val result = setState(job, JobState.RUNNING, JobState.WAITING)
        if (result) {
            lockDao.create(LockSpec(job))
        }
        return result
    }

    override fun stop(job: Job, finalState: JobState) : Boolean {
        val result = setState(job, finalState, JobState.RUNNING)
        if (result) {
            lockDao.deleteByJob(job.id)
        }
        return result
    }

    override fun getWaiting(limit: Int) : List<Job> {
        return jobDao.getWaiting(limit)
    }

    override fun getRunning() : List<Job> {
        return jobDao.getRunning()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobServiceImpl::class.java)
    }
}
