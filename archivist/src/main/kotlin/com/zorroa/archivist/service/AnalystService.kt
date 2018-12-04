package com.zorroa.archivist.service

import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.security.getAnalystEndpoint
import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystFilter
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.repository.KPagedList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface AnalystService {
    fun upsert(spec: AnalystSpec) : Analyst
    fun exists(endpoint: String) : Boolean
    fun getAll(filter: AnalystFilter) : KPagedList<Analyst>
    fun get(id: UUID) : Analyst
}

@Service
@Transactional
class AnalystServicImpl @Autowired constructor(val analystDao: AnalystDao): AnalystService {

    override fun upsert(spec: AnalystSpec) : Analyst {
        return if (analystDao.update(spec)) {
            val endpoint = getAnalystEndpoint()
            analystDao.get(endpoint!!)
        }
        else {
            val analyst = analystDao.create(spec)
            logger.info("Created analyst: {}", analyst.endpoint)
            analyst
        }

    }

    override fun exists(endpoint: String) : Boolean {
        return analystDao.exists(endpoint)
    }

    override fun get(id: UUID) : Analyst {
        return analystDao.get(id)
    }

    override fun getAll(filter: AnalystFilter) : KPagedList<Analyst> {
        return analystDao.getAll(filter)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DispatcherServiceImpl::class.java)
    }
}