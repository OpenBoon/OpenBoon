package com.zorroa.archivist.service

import com.zorroa.archivist.domain.EventLogSearch
import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.UserLogSpec
import com.zorroa.archivist.security.SecureSingleThreadExecutor
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

) : EventLogService {

    @Autowired
    private lateinit var jobService: JobService

    private val executor = SecureSingleThreadExecutor.singleThreadExecutor()

    override fun log(spec: UserLogSpec) {

    }

    override fun logAsync(spec: UserLogSpec) {

    }

    override fun log(task: Task, errors: List<TaskErrorT>) {

    }

    override fun logAsync(task: Task, errors: List<TaskErrorT>) {

    }

    override fun getAll(type: String, search: EventLogSearch): PagedList<Map<String, Any>> {
        return PagedList()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(EventLogServiceImpl::class.java)
    }
}
