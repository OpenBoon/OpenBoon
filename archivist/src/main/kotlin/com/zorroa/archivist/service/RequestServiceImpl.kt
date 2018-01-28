package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Request
import com.zorroa.archivist.domain.RequestSpec
import com.zorroa.archivist.repository.RequestDao
import com.zorroa.archivist.security.SecurityUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface RequestService {
    fun create(spec: RequestSpec) : Request
    fun get(id:Int) : Request
}

@Service
@Transactional
class RequestServiceImpl @Autowired constructor(
        private val requestDao: RequestDao,
        private val emailService: EmailService,
        private val tx: TransactionEventManager

) : RequestService  {

    override fun create(spec: RequestSpec) : Request {
        val req = requestDao.create(spec)
        tx.afterCommit(false, {
            emailService.sendExportRequestEmail(SecurityUtils.getUser(), req)
        })
        return req
    }

    override fun get(id:Int) : Request {
        return requestDao.get(id)
    }
}
