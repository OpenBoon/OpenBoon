package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.service.ExportService
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.common.domain.Job
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class ExportFileDaoTests : AbstractTest() {

    @Autowired
    private lateinit var exportService : ExportService

    @Autowired
    private lateinit var fileStorageService : FileStorageService

    @Autowired
    private lateinit var exportFileDao : ExportFileDao

    lateinit var export : Job
    lateinit var exportFile : ExportFile

    @Before
    fun init() {
        val espec = ExportSpec("foo",
                AssetSearch(),
                mutableListOf(),
                mutableMapOf("foo" to "bar"),
                mutableMapOf("foo" to "bar"))
        export = exportService.create(espec, resolve = false)

        val spec = FileStorageSpec("export", "foox", "bar",
                jobId=export.id)
        val storage = fileStorageService.get(spec)
        exportFile = exportFileDao.create(export, storage.getServableFile(),
                ExportFileSpec(storage.id, "foo.bar"))
    }

    @Test
    fun testGet() {
        val ef = exportFileDao.get(exportFile.id)
        assertEquals(exportFile.id, ef.id)
        assertEquals(exportFile.mimeType, ef.mimeType)
        assertEquals(exportFile.name, ef.name)
        assertEquals(exportFile.size, ef.size)
        assertEquals(exportFile.timeCreated, ef.timeCreated)
    }

    @Test
    fun testGetAll() {
        for (i in 1 .. 10) {
            val spec = FileStorageSpec("export", "foo$i", "bar",
                    jobId=export.id)
            val storage = fileStorageService.get(spec)
            exportFileDao.create(export, storage.getServableFile(), ExportFileSpec(storage.id, "foo$i.bar"))
        }
        assertEquals(11, exportFileDao.getAll(export).size)
        println(Json.prettyString(exportFileDao.getAll(export)))
    }
}
