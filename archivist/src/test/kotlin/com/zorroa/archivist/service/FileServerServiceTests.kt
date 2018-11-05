package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import java.net.URI
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class GcpFileServerServiceTests : AbstractTest() {

    @Test
    fun test() {
        val s = GcpFileServerService(properties, Paths.get("unittest/config/data-credentials.json"))
        val u = s.getSignedUrl(
                URI("gs://rmaas-us-dit2/company/25274/539be253-9b20-4c0f-9476-e8ccf596bca6/image/600164.pdf"))
        assertEquals("https", u.protocol)
        assertEquals("storage.googleapis.com", u.authority)
        assertTrue(u.query.contains("GoogleAccessId="))
        assertTrue(u.query.contains("&Signature="))
    }
}