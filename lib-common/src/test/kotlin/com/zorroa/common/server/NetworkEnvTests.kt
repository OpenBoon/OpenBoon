package com.zorroa.common.server

import junit.framework.Assert.assertEquals
import org.junit.Test

class NetworkEnvTests {

    @Test
    fun testGoogleAppEngineEnvironment() {
        val gae = GoogleAppEngineEnvironment("foo", mapOf("bing" to "https://localhost:1234"))
        // auto
        assertEquals("https://archivist-dot-foo.appspot.com", gae.getPublicUrl("archivist"))
        // override
        assertEquals("https://localhost:1234", gae.getPublicUrl("bing"))
    }

    @Test
    fun testStaticVMEnvironment() {
        val gae = StaticVmEnvironment("foo", mapOf("test" to "https://localhost:1234"))
        // auto
        assertEquals("https://foo-foo.zorroa.com", gae.getPublicUrl("foo"))
        // override
        assertEquals("https://localhost:1234", gae.getPublicUrl("test"))
    }

    @Test
    fun testDockerComposeEnvironment() {
        val gae = DockerComposeEnvironment(mapOf("test" to "https://localhost:1234"))
        // auto
        assertEquals("http://foo", gae.getPublicUrl("foo"))
        // override
        assertEquals("https://localhost:1234", gae.getPublicUrl("test"))
    }
}