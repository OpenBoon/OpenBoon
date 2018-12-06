package com.zorroa.archivist.web

import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.service.ExportService
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.common.domain.Job
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.nio.file.Files
import kotlin.test.assertEquals

class ExportControllerTests : MockMvcTest() {

    @Autowired
    lateinit var exportService: ExportService

    @Autowired
    lateinit var fileStorageService: FileStorageService

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
                .with(SecurityMockMvcRequestPostProcessors.csrf())
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

        val storage = fileStorageService.get(FileStorageSpec(
                "export", "foo", "txt", jobId=export.id))
        Files.write(storage.getServableFile().getLocalFile(), "bing".toByteArray())

        val fspec = ExportFileSpec(storage.id, "foo.txt")
        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/exports/${export.id}/_files")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(fspec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val file = Json.Mapper.readValue(req.response.contentAsString, ExportFile::class.java)
        println(Json.prettyString(file))
        assertEquals("export___${export.id}___foo.txt", file.path)
        assertEquals("text/plain", file.mimeType)
        assertEquals(4, file.size)

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


        val storage = fileStorageService.get(FileStorageSpec(
                "export", "foo", "txt", jobId=export.id))
        Files.write(storage.getServableFile().getLocalFile(), "bing".toByteArray())

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/exports/${export.id}/_files")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("storageId" to storage.id, "filename" to "exported.txt"))))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val file = Json.Mapper.readValue(req.response.contentAsString, ExportFile::class.java)

        val req2 = mvc.perform(MockMvcRequestBuilders.get("/api/v1/exports/${export.id}/_files/${file.id}/_stream")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.header().string("Content-Disposition",
                        "attachment; filename=\"exported.txt\""))
                .andExpect(MockMvcResultMatchers.header().longValue("Content-Length",
                        storage.getServableFile().getStat().size))
                .andExpect(MockMvcResultMatchers.header().string("Content-Type", "text/plain"))
                .andReturn()

        val content = req2.response.contentAsString
        assertEquals("bing", content)
    }
}
