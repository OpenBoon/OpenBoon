package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.processor.Source
import com.zorroa.sdk.search.AssetSearch
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class ExportServiceTests : AbstractTest() {

    @Autowired
    internal lateinit var jobExecutorService: JobExecutorService

    internal var spec: ExportSpec? = null
    internal var asset: Document? = null

    @Before
    fun init() {
        val source = Source(testImagePath.resolve("beer_kettle_01.jpg"))
        source.addKeywords("cats")
        asset = assetService.index(source)
        refreshIndex()
    }

    @Test
    fun testCreate() {
        val spec = ExportSpec("test",
                AssetSearch().setQuery("cats"),
                Lists.newArrayList<ProcessorRef>(),
                Maps.newHashMap(), false)

        val job = exportService.create(spec)
        val count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM task WHERE pk_job=?", Int::class.java, job.jobId)
        assertEquals(1, count.toLong())
    }

    @Test
    fun testCreateAndGetExportFile() {
        val spec = ExportSpec("test",
                AssetSearch().setQuery("cats"),
                Lists.newArrayList<ProcessorRef>(),
                Maps.newHashMap(), false)
        val job = exportService.create(spec)

        val file1 = exportService.createExportFile(job, ExportFileSpec(
                "foo.zip", "application/octet-stream", 100))
        val file2 = exportService.getExportFile(file1.id)
        assertEquals(file1, file2)
        assertEquals(file1.jobId, file2.jobId)
        assertEquals(file1.mimeType, file2.mimeType)
        assertEquals(file1.name, file2.name)
        assertEquals(file1.size, file2.size)
    }

    @Test
    fun testGetAllExportFiles() {

        val spec = ExportSpec("test",
                AssetSearch().setQuery("cats"),
                Lists.newArrayList<ProcessorRef>(),
                Maps.newHashMap(), false)

        val job = exportService.create(spec)
        for (i in 0..9) {
            exportService.createExportFile(job, ExportFileSpec(
                    "foo$i.zip", "application/octet-stream", 1024))
        }

        val files = exportService.getAllExportFiles(job)
        assertEquals(10, files.size.toLong())
    }
}
