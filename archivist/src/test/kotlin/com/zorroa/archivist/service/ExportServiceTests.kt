package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.common.domain.Job
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
                mutableMapOf("foo" to "bar"),
                compress=true)
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
        val storage = fileStorageService.get(FileStorageSpec("export",
                "foo", "txt", jobId=job.id))
        Files.write(storage.getServableFile().getLocalFile(), "a-team".toByteArray())
        val ex1 = exportService.createExportFile(job, ExportFileSpec(storage.id))
        assertEquals(1, exportService.getAllExportFiles(job).size)
    }

    @Test
    fun testValidateExportAssetSearch() {
        val search = AssetSearch()
        search.addToFilter().addToLinks("export", job.id)
        assertTrue(searchService.count(search) > 0)
    }
}
