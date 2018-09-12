package com.zorroa.archivist.web

import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.service.ExportService
import com.zorroa.common.domain.Job
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.nio.file.Files
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

        val export = Json.Mapper.readValue<Job>(req.response.contentAsString, Job::class.java)
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

    @Test
    @Throws(Exception::class)
    fun testGetExportFile() {
        val session = admin()
        addTestAssets("set04/standard")
        this.getTestImagePath()
        val espec = ExportSpec("foo",
                AssetSearch(),
                mutableListOf(),
                mutableMapOf("foo" to "bar"),
                mutableMapOf("foo" to "bar"),
                compress=true)
        val export = exportService.create(espec)

        val path = getTestImagePath().resolve("beer_kettle_01.jpg")
        val fspec = ExportFileSpec(path.toUri().toString(), "image/jpeg", Files.size(path))

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/exports/${export.id}/_files")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(fspec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val file = Json.Mapper.readValue(req.response.contentAsString, ExportFile::class.java)

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/exports/${export.id}/_files/${file.id}/_stream")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
    }
}
