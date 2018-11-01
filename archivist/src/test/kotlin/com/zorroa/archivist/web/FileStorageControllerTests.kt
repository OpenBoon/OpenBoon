package com.zorroa.archivist.web

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

        val fs = Json.Mapper.readValue(req.response.contentAsString, FileStorage::class.java)
        assertTrue(FileUtils.filename(fs.uri).endsWith(".jpg"))
        assertEquals("image/jpeg", fs.mimeType)
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

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/file-storage/_stat")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val stat = Json.Mapper.readValue(req.response.contentAsString, FileStorage::class.java)

        val req2 = mvc.perform(MockMvcRequestBuilders.get("/api/v1/file-storage/" + stat.id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val stat2 = Json.Mapper.readValue(req2.response.contentAsString, FileStorage::class.java)
        assertEquals(stat.id, stat2.id)
    }

    @Test
    fun testStat() {
        val session = admin()
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "100", "y", "100"))

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/file-storage/_stat")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val stat = Json.Mapper.readValue(req.response.contentAsString, FileStorage::class.java)
        assertEquals(-1, stat.size)
        assertEquals("image/jpeg", stat.mimeType)
        assertFalse(stat.exists)
    }
}