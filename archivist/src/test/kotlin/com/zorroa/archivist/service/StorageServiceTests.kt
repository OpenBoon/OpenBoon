package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.common.domain.Asset
import org.junit.Before
import org.junit.Test
import java.net.URL
import java.util.*
import kotlin.test.assertEquals

/**
 * Ignoring all these for now, most GCP methods are not enabled and actually
 * can probably be deleted.
 */

class StorageServiceTests : AbstractTest() {

    lateinit var asset: Asset

    @Before
    fun init() {
        asset = Asset(UUID.randomUUID(), UUID.randomUUID())
    }

    @Test
    fun testGetObjectFile() {
        val bs = storageService.getObjectFile(
                URL("https://storage.cloud.google.com/rmaas-us-dit1/100/ef0ca910-5c67-4b03-bb8e-c92e2d7e7283/file/proxy_ef0ca910-5c67-4b03-bb8e-c92e2d7e7283_197x256.jpg"))
        assertEquals("faces.jpg", bs.name)
        assertEquals(113333, bs.size)
        assertEquals("image/jpeg", bs.type)
    }
}
