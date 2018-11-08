package com.zorroa.archivist.web

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.TrashedFolder
import com.zorroa.archivist.domain.TrashedFolderOp
import com.zorroa.archivist.repository.TrashFolderDao
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MvcResult

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by chambers on 12/5/16.
 */
class TrashFolderControllerTests : MockMvcTest() {

    lateinit var deleteOp: TrashedFolderOp

    lateinit var folder1: Folder

    @Autowired
    internal var trashFolderDao: TrashFolderDao? = null

    @Before
    fun init() {
        folder1 = folderService.create(FolderSpec("test1"))
        val folder2 = folderService.create(FolderSpec("test2", folder1))
        folderService.create(FolderSpec("test3", folder2))
        deleteOp = folderService.trash(folder1)
    }

    @Test
    @Throws(Exception::class)
    fun testRestore() {
        val exists = jdbc.queryForObject("SELECT COUNT(1) FROM folder WHERE str_name='test1'", Int::class.java)!!
        assertEquals(0, exists.toLong())

        val content = Json.serializeToString(
                ImmutableList.of(deleteOp.trashFolderId.toString()))
        val result = mvc.perform(post("/api/v1/trash/_restore")
                .session(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(content)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()
        val data = deserialize(result, Map::class.java)
        assertTrue(data["success"] as Boolean)
    }

    @Test
    @Throws(Exception::class)
    fun testEmpty() {
        val exists = jdbc.queryForObject("SELECT COUNT(1) FROM folder_trash", Int::class.java)!!
        assertEquals(3, exists.toLong())

        val result = mvc.perform(delete("/api/v1/trash")
                .session(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val data = deserialize(result, Map::class.java)
        assertTrue(data["success"] as Boolean)

        val folders = trashFolderDao!!.getAll(admin().id)
        assertEquals(0, folders.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testEmptySpecificFolders() {
        val exists = jdbc.queryForObject("SELECT COUNT(1) FROM folder_trash", Int::class.java)!!
        assertEquals(3, exists.toLong())

        val result = mvc.perform(delete("/api/v1/trash")
                .session(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serializeToString(listOf(folder1.id)))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val data = deserialize(result, Map::class.java)
        assertTrue(data["success"] as Boolean)

        val folders = trashFolderDao!!.getAll(admin().id)
        assertEquals(0, folders.size.toLong())
    }


    @Test
    @Throws(Exception::class)
    fun testGetAll() {
        val result = mvc.perform(get("/api/v1/trash")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val data = deserialize(result, object : TypeReference<List<TrashedFolder>>() {

        })
        assertEquals(1, data.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testCount() {
        val result = mvc.perform(get("/api/v1/trash/_count")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val data = deserialize(result, Map::class.java)
        assertEquals(1, data["count"])
    }
}
