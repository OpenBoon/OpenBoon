package com.zorroa.archivist.web

import com.zorroa.archivist.domain.Export
import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.service.ExportService
import com.zorroa.common.search.AssetSearch
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals

class ExportControllerTests : MockMvcTest() {

    @Autowired
    lateinit var exportService: ExportService

    @Test
    @Throws(Exception::class)
    fun testCreate() {
        val session = admin()
        addTestAssets("set04/standard")

        val spec = ExportSpec("foo",
                AssetSearch(),
                mutableListOf(),
                mutableMapOf("foo" to "bar"),
                mutableMapOf("foo" to "bar"),
                compress=true)

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/exports")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val export = Json.Mapper.readValue<Export>(req.response.contentAsString, Export::class.java)
        assertEquals(spec.name, export.name)
    }


    @Test
    @Throws(Exception::class)
    fun testCreateExportFile() {
        val session = admin()
        addTestAssets("set04/standard")

        val espec = ExportSpec("foo",
                AssetSearch(),
                mutableListOf(),
                mutableMapOf("foo" to "bar"),
                mutableMapOf("foo" to "bar"),
                compress=true)
        val export = exportService.create(espec)

        val fspec = ExportFileSpec(
                "/tmp/foo.jpg", "image/jpeg", 1000000)

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/exports/${export.id}/_files")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(fspec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val file = Json.Mapper.readValue(req.response.contentAsString, ExportFile::class.java)
        assertEquals(fspec.path, file.path)
        assertEquals(fspec.mimeType, file.mimeType)
        assertEquals(fspec.size, file.size)
    }
}
