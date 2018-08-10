package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Export
import com.zorroa.archivist.domain.ExportFilter
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.domain.JobState
import com.zorroa.common.domain.ProcessorRef
import com.zorroa.common.repository.KPage
import com.zorroa.common.search.AssetSearch
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

data class ExportSpec (
        var name: String?,
        var search: AssetSearch,
        var processors: List<ProcessorRef> = mutableListOf(),
        var args: Map<String,Any> = mutableMapOf(),
        var compress: Boolean = true)
class ExortDaoTests : AbstractTest() {

    @Autowired
    private lateinit var exportDao : ExportDao

    lateinit var export : Export

    @Before
    fun init() {
        val spec = ExportSpec("foo",
                AssetSearch(),
                mutableListOf(),
                mutableMapOf("foo" to "bar"),
                mutableMapOf("foo" to "bar"),
                compress=true)
        export = exportDao.create(spec)
    }

    @Test
    fun testGet() {
        val ex1 = exportDao.get(export.id)
        assertEquals(ex1.id, export.id)
        assertEquals(ex1.name, export.name)
        assertEquals(ex1.organizationId, export.organizationId)
    }

    @Test
    fun testGetAll() {
        for (i in 1 .. 10) {
            val spec = ExportSpec("foo$i",
                    AssetSearch(),
                    mutableListOf(),
                    compress=true)
            exportDao.create(spec)
        }

        // TODO: add tests for other filters
        val result = exportDao.getAll(KPage(), ExportFilter())
        // +1 made by init()
        assertEquals(11, result.size())
    }

    @Test
    fun testCount() {
        for (i in 1 .. 10) {
            val spec = ExportSpec("foo$i",
                    AssetSearch(),
                    mutableListOf(),
                    compress=true)
            exportDao.create(spec)
        }
        assertEquals(11, exportDao.count())
    }

    @Test
    fun testSetState() {
        val ex1 = exportDao.get(export.id)
        assertTrue(exportDao.setState(ex1.id, JobState.Finished))
        assertFalse(exportDao.setState(ex1.id, JobState.Finished))
        assertTrue(exportDao.setState(ex1.id, JobState.Active))
        assertFalse(exportDao.setState(ex1.id, JobState.Active))
    }

}
