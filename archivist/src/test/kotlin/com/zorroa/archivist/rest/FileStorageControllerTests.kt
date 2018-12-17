package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.service.FileStat
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileStorageControllerTests : MockMvcTest() {

    @Test
    fun testCreate() {
        val session = admin()
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "100", "y", "100"))

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/file-storage")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
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
        val session = admin()
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "100", "y", "100"))

        val req1 = mvc.perform(MockMvcRequestBuilders.post("/api/v1/file-storage")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val rsp1 = Json.Mapper.readValue(req1.response.contentAsString, FileStorageResponse::class.java)


        val req2 = mvc.perform(MockMvcRequestBuilders.get("/api/v1/file-storage/${rsp1.id}")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val rsp2 = Json.Mapper.readValue(req2.response.contentAsString, FileStorageResponse::class.java)

        assertEquals(rsp1.id, rsp2.id)
    }

    @Test
    fun testStat() {
        val session = admin()
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "100", "y", "100"))


        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/file-storage")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val fs = Json.Mapper.readValue(req.response.contentAsString, FileStorageResponse::class.java)
        val id = fs.id

        val req2 = mvc.perform(MockMvcRequestBuilders.get("/api/v1/file-storage/$id/_stat")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val stat = Json.Mapper.readValue(req2.response.contentAsString, FileStat::class.java)
        assertEquals("image/jpeg", stat.mediaType)
    }
}

class FileStorageResponse (
        val id: String,
        val uri: String,
        val scheme: String,
        val mediaType: String
) {
    override fun toString(): String {
        return "FileStorage(uri='$uri', id='$id', scheme='$scheme', mimeType='$mediaType')"
    }
}
