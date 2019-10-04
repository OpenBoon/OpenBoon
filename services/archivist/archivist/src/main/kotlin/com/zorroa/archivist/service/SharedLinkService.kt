package com.zorroa.archivist.service

import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.repository.SharedLinkDao
import com.zorroa.archivist.security.getUserId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

/**
 * SharedLinkService allows users to create a Link to curator they can pass around.
 *
 * It is used.
 */
interface SharedLinkService {

    /**
     * Create and return a new [SharedLink]
     *
     * @param spec: A SharedLink specification.
     */
    fun create(spec: SharedLinkSpec): SharedLink

    /**
     * Remove SharedLinks that have around for the given duration. Return
     * the number of SharedLinks returned.
     *
     * @param duration a [Duration] which describes how old the link has to be,.
     */
    fun deleteExpired(duration: Duration): Int

    /**
     * Return a SharedLink with the given Id.
     */
    fun get(id: UUID): SharedLink
}

@Service
@Transactional
class SharedLinkServiceImpl @Autowired constructor(
    private val sharedLinkDao: SharedLinkDao,
    private val transactionEventManager: TransactionEventManager
) : SharedLinkService {

    @Autowired
    internal lateinit var emailService: EmailService

    @Autowired
    internal lateinit var userService: UserService

    override fun create(spec: SharedLinkSpec): SharedLink {
        val link = sharedLinkDao.create(spec)
        val fromUser = userService.get(getUserId())

        transactionEventManager.afterCommit(false) {
            spec.userIds?.forEach {
                try {
                    val toUser = userService.get(it)
                    emailService.sendSharedLinkEmail(fromUser, toUser, link)
                } catch (e: Exception) {
                    logger.warn("Failed to send shared link email, id {} ", link.id, e)
                }
            }
        }

        return link
    }

    override fun get(id: UUID): SharedLink {
        return sharedLinkDao.get(id)
    }

    override fun deleteExpired(duration: Duration): Int {
        return sharedLinkDao.deleteExpired(duration)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SharedLinkServiceImpl::class.java)
    }
}
