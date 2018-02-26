package com.zorroa.archivist.service

import com.google.common.base.Preconditions
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

    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var userService: UserService


    override fun create(spec: RequestSpec) : Request {
        Preconditions.checkNotNull(spec.folderId, "The folderId for a request cannot be null")
        Preconditions.checkNotNull(spec.type, "The type for a request cannot be null")

        // Validate folder exists.
        folderService.get(spec.folderId!!)

        val req = requestDao.create(spec)
        tx.afterCommit(false, {
            val user = userService.get(SecurityUtils.getUser().id)
            emailService.sendExportRequestEmail(user, req)
        })
        return req
    }

    override fun get(id:Int) : Request {
        return requestDao.get(id)
    }
}
