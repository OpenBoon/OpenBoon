package com.zorroa.archivist.web

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.FileStorageStat
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
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
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val fs = Json.Mapper.readValue(req.response.contentAsString, FileStorage::class.java)
        assertTrue(FileUtils.filename(fs.stream).endsWith(".jpg"))
        assertEquals("image/jpeg", fs.mimeType)
        assertEquals("file", fs.scheme)
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
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val stat = Json.Mapper.readValue(req.response.contentAsString, FileStorageStat::class.java)
        assertEquals(-1, stat.size)
        assertEquals("image/jpeg", stat.mimeType)
        assertFalse(stat.exists)
    }
}