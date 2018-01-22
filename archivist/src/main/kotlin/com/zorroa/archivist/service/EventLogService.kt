package com.zorroa.archivist.service

import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.EventLogSearch
import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.TaskStatsAdder
import com.zorroa.archivist.domain.UserLogSpec
import com.zorroa.archivist.repository.EventLogDao
import com.zorroa.archivist.security.SecureSingleThreadExecutor
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.cluster.thrift.TaskErrorT
import com.zorroa.sdk.domain.PagedList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

interface EventLogService {

    fun log(spec: UserLogSpec)

    fun logAsync(spec: UserLogSpec)

    fun log(task: Task, errors: List<TaskErrorT>)

    fun logAsync(task: Task, errors: List<TaskErrorT>)

    fun getAll(type: String, search: EventLogSearch): PagedList<Map<String, Any>>

}

/**
 *
 */
@Service
class EventLogServiceImpl @Autowired constructor(
        val eventLogDao: EventLogDao
) : EventLogService {

    @Autowired
    private lateinit var jobService: JobService

    private val executor = SecureSingleThreadExecutor.singleThreadExecutor()

    override fun log(spec: UserLogSpec) {
        if (spec.user == null) {
            try {
                val user = SecurityUtils.getUser()
                spec.setUser(user)
            } catch (e: Exception) {
                logger.warn("No Security Context {} ", spec.message, e)
            }

        }
        eventLogDao.create(spec)
    }

    override fun logAsync(spec: UserLogSpec) {
        if (ArchivistConfiguration.unittest) {
            log(spec)
        } else {
            executor.execute { log(spec) }
        }
    }

    override fun log(task: Task, errors: List<TaskErrorT>) {
        eventLogDao.create(task, errors)
        val adder = TaskStatsAdder()
        adder.error = errors.size

        jobService.incrementStats(task, adder)
    }

    override fun logAsync(task: Task, errors: List<TaskErrorT>) {
        if (ArchivistConfiguration.unittest) {
            log(task, errors)
        } else {
            executor.execute { log(task, errors) }
        }
    }

    override fun getAll(type: String, search: EventLogSearch): PagedList<Map<String, Any>> {
        return eventLogDao.getAll(type, search)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(EventLogServiceImpl::class.java)
    }
}
