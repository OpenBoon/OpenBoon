package com.zorroa.archivist.service

import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.repository.SharedLinkDao
import com.zorroa.archivist.security.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

interface SharedLinkService {
    fun create(spec: SharedLinkSpec): SharedLink

    operator fun get(id: Int): SharedLink
}

@Service
class SharedLinkServiceImpl @Autowired constructor(
        private val sharedLinkDao: SharedLinkDao,
        private val transactionEventManager: TransactionEventManager
) : SharedLinkService {

    @Autowired
    internal lateinit var userService: UserService

    override fun create(spec: SharedLinkSpec): SharedLink {
        val link = sharedLinkDao.create(spec)
        val fromUser = SecurityUtils.getUser()

        if (spec.isSendEmail) {
            transactionEventManager.afterCommit(false, {
                for (userId in spec.userIds) {
                    try {
                        val toUser = userService.get(userId)
                        userService.sendSharedLinkEmail(fromUser, toUser, link)
                    } catch (e: Exception) {
                        logger.warn("Failed to send shared link email, id {} ", link.id, e)
                    }

                }
            })
        }
        return link
    }

    override fun get(id: Int): SharedLink {
        return sharedLinkDao.get(id)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SharedLinkServiceImpl::class.java)
    }
}
