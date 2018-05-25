package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.domain.PipelineSpecV
import com.zorroa.archivist.domain.TaskState
import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.processor.Source
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.util.Json
import com.zorroa.sdk.zps.ZpsScript
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class ExportServiceTests : AbstractTest() {

    internal var spec: ExportSpec? = null
    internal var asset: Document? = null

    @Before
    fun init() {
        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.addToKeywords("media", "cats")
        asset = indexService.index(source)
        refreshIndex()
    }

    @Test
    fun testCreate() {
        val spec = ExportSpec("test",
                AssetSearch(),
                Lists.newArrayList<ProcessorRef>(),
                Maps.newHashMap(), false)

        val job = exportService.create(spec)
        val count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM task WHERE pk_job=?", Int::class.java, job.jobId)
        assertEquals(1, count.toLong())
    }


    @Test
    fun testCreateWithAppendedPipeline() {

        System.setProperty("archivist.export.appendPipelines", "test")
        try {
            val pspec = PipelineSpecV()
            pspec.processors = Lists.newArrayList(
                    ProcessorRef("com.zorroa.core.common.GroupProcessor")
                            .setArg("bing", "bong"))

            pspec.description = "A NoOp"
            pspec.name = "test"
            pspec.type = PipelineType.Export
            pipelineService.create(pspec)

            val spec = ExportSpec("test",
                    AssetSearch(),
                    Lists.newArrayList<ProcessorRef>(
                            ProcessorRef("com.zorroa.core.common.GroupProcessor")
                                    .setArg("foo", "bar")),
                    Maps.newHashMap(), false)

            val job = exportService.create(spec)
            val task = jobService.getTasks(job.id, TaskState.Waiting)
            val script = Json.Mapper.readValue(File(task[0].scriptPath), ZpsScript::class.java)

            assertEquals("bar", script.execute[0].args["foo"])
            assertEquals("bong", script.execute[1].args["bing"])

        } finally {
            System.clearProperty("archivist.export.appendPipelines")
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateWithAppendedPipelineFailure() {

        System.setProperty("archivist.export.appendPipelines", "test")
        try {
            val pspec = PipelineSpecV()
            pspec.processors = Lists.newArrayList(
                    ProcessorRef("com.zorroa.core.common.GroupProcessor")
                            .setArg("bing", "bong"))

            pspec.description = "A NoOp"
            pspec.name = "test"
            // WRONG PIPELINE TYPE
            pspec.type = PipelineType.Import
            pipelineService.create(pspec)

            val spec = ExportSpec("test",
                    AssetSearch(),
                    Lists.newArrayList<ProcessorRef>(
                            ProcessorRef("com.zorroa.core.common.GroupProcessor")
                                    .setArg("foo", "bar")),
                    Maps.newHashMap(), false)

            exportService.create(spec)

        } finally {
            System.clearProperty("archivist.export.appendPipelines")
        }
    }


    @Test
    fun testCreateAndGetExportFile() {
        val spec = ExportSpec("test",
                AssetSearch(),
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
                AssetSearch(),
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
