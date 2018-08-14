package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Export
import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.common.search.AssetSearch
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class ExportFileDaoTests : AbstractTest() {

    @Autowired
    private lateinit var exportDao : ExportDao

    @Autowired
    private lateinit var exportFileDao : ExportFileDao

    lateinit var export : Export
    lateinit var exportFile : ExportFile

    @Before
    fun init() {
        val espec = ExportSpec("foo",
                AssetSearch(),
                mutableListOf(),
                mutableMapOf("foo" to "bar"),
                mutableMapOf("foo" to "bar"),
                compress=true)
        export = exportDao.create(espec)

        val spec = ExportFileSpec("/tmp/foo.bar",
                "application/x-bar",
                1024)

        exportFile = exportFileDao.create(export, spec)
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
            val spec = ExportFileSpec("/tmp/foo$i.bar",
                    "application/x-bar",
                    1024 + (i * 2L))
            exportFileDao.create(export, spec)
        }
        assertEquals(11, exportFileDao.getAll(export).size)
    }
}
