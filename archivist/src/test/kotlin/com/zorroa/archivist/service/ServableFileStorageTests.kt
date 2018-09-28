package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI
import kotlin.test.assertTrue

class ServableFileStorageTests : AbstractTest() {

    @Test
    fun getLocalFileStat() {
        val ofile = fileServerProvider.getServableFile(URI("file:///Users/chambers/src/zorroa-test-data/video/sample_ipad.m4v"))
        assertTrue(ofile.getStat().exists)
    }
}
