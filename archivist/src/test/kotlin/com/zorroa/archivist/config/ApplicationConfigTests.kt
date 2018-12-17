package com.zorroa.archivist.config

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class ApplicationConfigTests : AbstractTest() {

    @Autowired
    lateinit var networkEnvironment: NetworkEnvironment

    @Test
    fun testNetworkEnvironment() {
        // host override
        assertEquals("https://10.0.1.2", networkEnvironment.getPublicUrl("example-service"))
        assertEquals("http://archivist", networkEnvironment.getPublicUrl("archivist"))
    }

}