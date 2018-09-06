package com.zorroa.archivist.service

import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface AnalystService {
    fun upsert(spec: AnalystSpec) : Analyst
    fun exists(endpoint: String) : Boolean
}

@Service
@Transactional
class AnalystServicImpl @Autowired constructor(val analystDao: AnalystDao): AnalystService {

    override fun upsert(spec: AnalystSpec) : Analyst {
        return if (analystDao.update(spec)) {
            analystDao.get(spec.endpoint)

        }
        else {
            analystDao.create(spec)
        }
    }

    override fun exists(endpoint: String) : Boolean {
        return analystDao.exists(endpoint)
    }
}