package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.service.FileStat
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.archivist.util.FileUtils
import com.zorroa.archivist.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.nio.file.Files
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileStorageControllerTests : MockMvcTest() {

    @Autowired
    lateinit var fileStorageService: FileStorageService

    @Test
    fun testCreate() {
        val spec = FileStorageSpec(
                "asset",
                UUID.randomUUID().toString(),
                "so_urgent_x_100_y_100.jpg")

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/file-storage")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val fs = Json.Mapper.readValue(req.response.contentAsString, FileStorageResponse::class.java)
        assertTrue(FileUtils.filename(fs.uri).endsWith(".jpg"))
        assertEquals("image/jpeg", fs.mediaType)
        assertEquals("file", fs.scheme)
    }

    @Test
    fun testGetById() {
        val spec = FileStorageSpec(
                "asset",
                UUID.randomUUID().toString(),
                "so_urgent_x_100_y_100.jpg")

        val req1 = mvc.perform(MockMvcRequestBuilders.post("/api/v1/file-storage")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val rsp1 = Json.Mapper.readValue(req1.response.contentAsString, FileStorageResponse::class.java)

        val req2 = mvc.perform(MockMvcRequestBuilders.get("/api/v1/file-storage/${rsp1.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val rsp2 = Json.Mapper.readValue(req2.response.contentAsString, FileStorageResponse::class.java)

        assertEquals(rsp1.id, rsp2.id)
    }

    @Test
    fun testStat() {
        val spec = FileStorageSpec(
                "asset",
                UUID.randomUUID().toString(),
                "so_urgent_x_100_y_100.jpg")

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/file-storage")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val fs = Json.Mapper.readValue(req.response.contentAsString, FileStorageResponse::class.java)
        val id = fs.id

        val req2 = mvc.perform(MockMvcRequestBuilders.get("/api/v1/file-storage/$id/_stat")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val stat = Json.Mapper.readValue(req2.response.contentAsString, FileStat::class.java)
        assertEquals("image/jpeg", stat.mediaType)
    }

    @Test
    fun testStream() {
        val spec = FileStorageSpec(
            "asset",
            UUID.randomUUID().toString(),
            "so_urgent.txt")

        val st = fileStorageService.get(spec)
        val localFile = st.getServableFile().getLocalFile()
        Files.createDirectories(localFile?.parent)
        Files.write(localFile, listOf("bob"))

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/file-storage/${st.id}/_stream")
            .headers(admin())
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType("text/plain"))
            .andExpect(MockMvcResultMatchers.content().string("bob\n"))
            .andReturn()
    }
}

class FileStorageResponse(
    val id: String,
    val uri: String,
    val scheme: String,
    val mediaType: String
) {
    override fun toString(): String {
        return "FileStorage(uri='$uri', id='$id', scheme='$scheme', mimeType='$mediaType')"
    }
}
