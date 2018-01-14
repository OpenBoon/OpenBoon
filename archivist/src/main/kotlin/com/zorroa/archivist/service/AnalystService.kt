package com.zorroa.archivist.service

import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

interface AnalystService {

    fun getCount(): Int

    fun getActive(): List<Analyst>

    fun getAll(paging: Pager): PagedList<Analyst>

    fun register(spec: AnalystSpec)

    fun get(url: String): Analyst
}

@Service
class AnalystServiceImpl
    @Autowired constructor (
        private val analystDao: AnalystDao,
        private val taskDao: TaskDao)  : AnalystService {

    override fun register(spec: AnalystSpec) {
        analystDao.register(spec)
        if (spec.taskIds != null) {
            taskDao.updatePingTime(spec.taskIds)
            if (logger.isDebugEnabled) {
                logger.debug("updated {} task Ids for {}", spec.taskIds, spec.url)
            }
        }
    }

    override operator fun get(url: String): Analyst {
        return analystDao[url]
    }

    override fun getCount(): Int {
        return Math.toIntExact(analystDao.count())
    }

    override fun getActive(): List<Analyst> {
        return analystDao.getActive(Pager(1, 100))
    }

    override fun getAll(paging: Pager): PagedList<Analyst> {
        return analystDao.getAll(paging)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystServiceImpl::class.java)
    }
}
