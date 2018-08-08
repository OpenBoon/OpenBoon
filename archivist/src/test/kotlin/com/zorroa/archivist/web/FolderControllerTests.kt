package com.zorroa.archivist.web

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.FolderUpdate
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.web.api.FolderController
import com.zorroa.common.domain.Pager
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Collectors
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FolderControllerTests : MockMvcTest() {

    @Autowired
    internal lateinit var folderController: FolderController

    @Autowired
    internal lateinit var assetDao: IndexDao

    internal lateinit var folder: Folder

    internal lateinit var session: MockHttpSession

    @Before
    @Throws(Exception::class)
    fun init() {
        session = admin()


        val spec = FolderSpec("TestFolder1")
        val s = Json.prettyString(spec)
        logger.info(s)
        val spec2 = Json.deserialize(s, FolderSpec::class.java)


        val result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(FolderSpec("TestFolder1"))))
                .andExpect(status().isOk())
                .andReturn()

        folder = Json.Mapper.readValue(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })
    }

    @Test
    @Throws(Exception::class)
    fun testCreate() {
        val result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(FolderSpec("TestFolder2"))))
                .andExpect(status().isOk())
                .andReturn()
        val (_, name) = Json.Mapper.readValue<Folder>(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })
        assertEquals("TestFolder2", name)
    }

    @Test
    @Throws(Exception::class)
    fun testGet() {
        val result = mvc.perform(get("/api/v1/folders/" + folder.id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()
        val (id, name, parentId, _, _, user) = Json.Mapper.readValue<Folder>(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })

        assertEquals(folder.id, id)
        assertEquals(folder.parentId, parentId)
        assertEquals(folder.user, user)
        assertEquals(folder.name, name)
    }

    @Test
    @Throws(Exception::class)
    fun testGetByPath() {
        val result = mvc.perform(get("/api/v1/folders/_path/Users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()
        val (_, name, parentId) = Json.Mapper.readValue<Folder>(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })

        assertEquals("Users", name)
        //assertEquals(getRootFolderId(), parentId)
    }

    @Test
    @Throws(Exception::class)
    fun testGetByPathV2() {
        authenticate("admin")
        folderService.create(FolderSpec("  foo  "))
        val result = mvc.perform(get("/api/v2/folders/_getByPath")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of("path", "/  foo  "))))
                .andExpect(status().isOk())
                .andReturn()
        val (_, name, parentId) = Json.Mapper.readValue<Folder>(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })

        assertEquals("  foo  ", name)
        //assertEquals(getRootFolderId(), parentId)
    }

    @Test
    @Throws(Exception::class)
    fun testExistsByPathV2() {
        val result = mvc.perform(get("/api/v2/folders/_existsByPath")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of("path", "/Users"))))
                .andExpect(status().isOk())
                .andReturn()

        val rs = deserialize(result, MockMvcTest.StatusResult::class.java)
        assertTrue(rs.success)
    }


    @Test
    @Throws(Exception::class)
    fun testExitsByPath() {
        val result = mvc.perform(get("/api/v1/folders/_exists/Users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()
        val rs = deserialize(result, MockMvcTest.StatusResult::class.java)
        assertTrue(rs.success)
    }

    @Test
    @Throws(Exception::class)
    fun testExitsByPathFailure() {
        val result = mvc.perform(get("/api/v1/folders/_exists/blah")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()
        val rs = deserialize(result, MockMvcTest.StatusResult::class.java)
        assertFalse(rs.success)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAll() {
        val result = mvc.perform(get("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()

        val folders = Json.Mapper.readValue<List<Folder>>(result.response.contentAsString,
                object : TypeReference<List<Folder>>() {

                })
        assertTrue(folders.contains(folder))
    }

    @Test
    @Throws(Exception::class)
    fun testUpdate() {

        var result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(FolderSpec("TestFolder3"))))
                .andExpect(status().isOk())
                .andReturn()

        val createdFolder = Json.Mapper.readValue<Folder>(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })
        val req = Json.Mapper.convertValue<Map<String, Any>>(createdFolder, Json.GENERIC_MAP)

        val up = FolderUpdate(createdFolder)
        up.attrs =ImmutableMap.of("a", "b")

        result = mvc.perform(put("/api/v1/folders/" + createdFolder.id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(createdFolder)))
                .andExpect(status().isOk())
                .andReturn()

        val (_, name, parentId) = Json.Mapper.readValue<Folder>(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })
        assertEquals(createdFolder.name, name)
        assertEquals(folder.parentId, parentId)
    }

    @Test
    @Throws(Exception::class)
    fun testDelete() {

        mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(FolderSpec("first"))))
                .andExpect(status().isOk())
                .andReturn()

        var result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(FolderSpec("second"))))
                .andExpect(status().isOk())
                .andReturn()

        val (id) = Json.Mapper.readValue<Folder>(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })

        mvc.perform(delete("/api/v1/folders/$id")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()

        result = mvc.perform(get("/api/v1/folders")
                .session(session))
                .andExpect(status().isOk())
                .andReturn()

        val folders = Json.Mapper.readValue<List<Folder>>(result.response.contentAsString,
                object : TypeReference<List<Folder>>() {

                })

        val names = folders.stream().map<String>( { it.name }).collect(Collectors.toSet())

        assertTrue(names.contains("first"))
        assertFalse(names.contains("second"))
        assertTrue(names.contains(folder.name))
    }

    @Test
    @Throws(Exception::class)
    fun testChildren() {

        var result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(FolderSpec("grandpa"))))
                .andExpect(status().isOk())
                .andReturn()

        val grandpa = Json.Mapper.readValue<Folder>(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })

        result = mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(FolderSpec("daddy", grandpa))))
                .andExpect(status().isOk())
                .andReturn()

        val dad = Json.Mapper.readValue<Folder>(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })

        mvc.perform(post("/api/v1/folders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(FolderSpec("uncly", grandpa))))
                .andExpect(status().isOk())
                .andReturn()

        result = mvc.perform(get("/api/v1/folders/" + grandpa.id + "/_children")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()

        val folders = Json.Mapper.readValue<List<Folder>>(result.response.contentAsString,
                object : TypeReference<List<Folder>>() {

                })

        assertEquals(2, folders.size.toLong())
        val names = folders.map { it.name}

        assertTrue(names.contains("daddy"))
        assertTrue(names.contains("uncly"))

        val up = FolderUpdate(dad)
        up.name = "daddy2"

        result = mvc.perform(put("/api/v1/folders/" + dad.id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(up)))
                .andExpect(status().isOk())
                .andReturn()
        val (id, _, parentId) = Json.Mapper.readValue<Folder>(result.response.contentAsString,
                object : TypeReference<Folder>() {

                })

        assertEquals(grandpa.id, parentId)
        assertEquals(dad.id, id)
    }

    @Test
    @Throws(Exception::class)
    fun testAddAsset() {
        authenticate("admin")

        addTestAssets("set04/standard")
        var assets = assetDao.getAll(Pager.first())

        val (id) = folderService.create(FolderSpec("foo"))

        val session = admin()
        mvc.perform(post("/api/v1/folders/$id/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map({ it.id}).collect(Collectors.toList()))))
                .andExpect(status().isOk())
                .andReturn()

        refreshIndex()
        authenticate("admin")
        assets = assetDao.getAll(Pager.first())
        for (asset in assets) {
            val links = asset.getAttr("zorroa.links.folder", object : TypeReference<List<Any>>() {

            })
            assertEquals(links[0], id.toString())
        }
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveAsset() {
        authenticate()

        addTestAssets("set04/standard")
        var assets = assetDao.getAll(Pager.first())

        val folder1 = folderService.create(FolderSpec("foo"))
        folderService.addAssets(folder1, assets.stream().map( { it.id.toString() }).collect(Collectors.toList()))
        refreshIndex()

        val session = admin()
        mvc.perform(delete("/api/v1/folders/" + folder1.id + "/assets")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map({ it.id }).collect(Collectors.toList()))))
                .andExpect(status().isOk())
                .andReturn()

        refreshIndex()
        authenticate("admin")
        assets = assetDao.getAll(Pager.first())
        for (asset in assets) {
            val links = asset.getAttr("zorroa.links.folder", object : TypeReference<List<Any>>() {

            })
            assertEquals(0, links.size.toLong())
        }
    }
}
