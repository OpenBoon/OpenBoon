package com.zorroa.archivist.service

import com.google.cloud.storage.HttpMethod
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.TaskState
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ExportServiceTests : AbstractTest() {

    @Autowired
    lateinit var exportService : ExportService

    @Autowired
    lateinit var taskDao : TaskDao

    @Autowired
    lateinit var fileStorageService: FileStorageService

    lateinit var job: Job

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()

        val spec = ExportSpec("foo",
                AssetSearch(),
                mutableListOf(),
                mutableMapOf("foo" to "bar"),
                mutableMapOf("foo" to "bar"))
        job = exportService.create(spec)
    }

    @Test
    fun testGetAll(){
        val ex1 = exportService.getAll(Pager.first())
        assertEquals(1, ex1.size())
    }

    @Test
    fun testCreatExportFile() {
        assertEquals(0, exportService.getAllExportFiles(job).size)
        val storage = fileStorageService.get(FileStorageSpec("job",
                job.id, "exported/foo.txt"))
        fileStorageService.getSignedUrl(storage.id, HttpMethod.PUT)

        Files.write(storage.getServableFile().getLocalFile(), "a-team".toByteArray())
        val ex1 = exportService.createExportFile(job, ExportFileSpec(storage.id, "bing.txt"))
        assertEquals(1, exportService.getAllExportFiles(job).size)
    }

    @Test
    fun testResolvePipeline() {
        val spec = ExportSpec("foo",
                AssetSearch(),
                mutableListOf(ProcessorRef("pipeline:standard-export")),
                mutableMapOf("foo" to "bar"),
                mutableMapOf("foo" to "bar"))
        job = exportService.create(spec)
        val task = taskDao.getAll(job.id, TaskState.Waiting)[0]
        val script = taskDao.getScript(task.id)
        assertEquals("zplugins.export.processors.ImageExporter", script.execute!![0].className)


    }
}
