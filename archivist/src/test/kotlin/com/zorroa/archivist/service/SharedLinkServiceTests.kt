package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.security.getUserId
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

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

        val spec = SharedLinkSpec(mapOf("foo" to "bar"))
        spec.userIds = setOf(getUserId())
        sharedLinkService.create(spec)
    }
}
