package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Export
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.mock.MockAnalystClient
import com.zorroa.archivist.security.getUser
import com.zorroa.common.clients.AnalystClient
import com.zorroa.common.search.AssetSearch
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ExportServiceTests : AbstractTest() {

    @Autowired
    lateinit var exportService : ExportService

    @Autowired
    lateinit var analystClient: AnalystClient

    lateinit var export: Export

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
        export = exportService.create(spec)
    }

    @Test
    fun testGet(){
        val ex1 = exportService.get(export.id)
        assertEquals(export.id, ex1.id)
        assertEquals(export.organizationId, ex1.organizationId)
        assertEquals(export.name, ex1.name)
        assertEquals(export.userId, ex1.userId)
    }

    @Test
    fun testValidateScriptEnv() {
        val ma = analystClient as MockAnalystClient
        val ex1 = exportService.get(export.id)
        val user = getUser()

        assertEquals("bar", ma.lastSpec!!.env["foo"])
        assertEquals(user.organizationId.toString(), ma.lastSpec!!.env["ZORROA_ORGANIZATION_ID"])
        assertEquals(ex1.id.toString(), ma.lastSpec!!.env["ZORROA_EXPORT_ID"])
    }

    @Test
    fun testValidateExportAssetSearch() {
        val search = AssetSearch()
        search.addToFilter().addToLinks("export", export.id)
        assertTrue(searchService.count(search) > 0)
    }

//    @Test
//    fun testBuildZpsScript() {
//        val export = Export(
//                id = UUID.randomUUID(),
//                organizationId = UUID.randomUUID(),
//                userId = UUID.randomUUID(),
//                name = "7eleven",
//                timeCreated = measureTimeMillis { LocalDateTime.now() }
//        )
//        val exportSpec = ExportSpec(
//                name = "some export",
//                search = AssetSearch()
//        )
//        script = exportService.buildZpsSciript(export, exportSpec)
//    }
}
