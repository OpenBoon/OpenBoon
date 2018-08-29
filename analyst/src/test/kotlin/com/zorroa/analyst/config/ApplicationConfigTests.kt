package com.zorroa.analyst.config

import com.zorroa.analyst.AbstractTest
import com.zorroa.common.server.NetworkEnvironment
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class ApplicationConfigTests : AbstractTest() {

    @Autowired
    lateinit var networkEnvironment: NetworkEnvironment

    @Test
    fun testNetworkEnvironment() {
        assertEquals("https://10.0.1.2",
                networkEnvironment.getPublicUrl("example-service"))
        assertEquals("https://boring-service",
                networkEnvironment.getPublicUrl("boring-service"))
    }

}