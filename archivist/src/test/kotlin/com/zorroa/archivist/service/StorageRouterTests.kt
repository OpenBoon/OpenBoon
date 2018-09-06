package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import java.net.URI
import kotlin.test.assertTrue

class StorageRouterTests : AbstractTest() {

    @Test
    fun getLocalFileStat() {
        val ofile = storageRouter.getObjectFile(URI("file:///Users/chambers/src/zorroa-test-data/video/sample_ipad.m4v"))
        assertTrue(ofile.getStat().exists)
    }
}
