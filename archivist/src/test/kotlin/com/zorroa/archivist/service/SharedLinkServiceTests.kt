package com.zorroa.archivist.service

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.service.SharedLinkService
import com.zorroa.archivist.service.TransactionEventManager
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import com.zorroa.archivist.security.getUserId

/**
 * Created by chambers on 7/11/17.
 */
class SharedLinkServiceTests : AbstractTest() {

    @Autowired
    lateinit var sharedLinkService: SharedLinkService

    @Test
    @Throws(InterruptedException::class)
    fun testSendEmail() {
        transactionEventManager.isImmediateMode = true

        val spec = SharedLinkSpec()
        spec.isSendEmail = true
        spec.state = mapOf("foo" to "bar")
        spec.userIds = setOf(getUserId())
        spec.expireTimeMs = 1L
        val link = sharedLinkService.create(spec)
    }
}
