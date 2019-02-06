package com.zorroa.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.service.FieldSystemService
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*
import java.util.stream.Collectors
import kotlin.streams.toList

class AssetControllerTests : MockMvcTest() {

    @Autowired
    lateinit var indexDao: IndexDao

    @Autowired
    lateinit var fieldSystemService: FieldSystemService

    @Before
    fun init() {
        fieldService.invalidateFields()
    }

    @After
    fun after() {
        SecurityContextHolder.getContext().authentication = null
    }

    @Test
    @Throws(Exception::class)
    fun testGetFields() {

        val session = admin()
        addTestAssets("set04/standard")

        val result = mvc.perform(get("/api/v1/assets/_fields")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val fields = Json.Mapper.readValue<Map<String, Set<String>>>(result.response.contentAsString,
                object : TypeReference<Map<String, Set<String>>>() {

                })
        assertTrue(fields["date"]!!.isNotEmpty())
        assertTrue(fields["string"]!!.isNotEmpty())
        assertTrue(fields.containsKey("integer"))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchV3() {

        val session = admin()
        addTestAssets("set04/standard")

        val result = mvc.perform(post("/api/v3/assets/_search")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(AssetSearch("O'Malley"))))
                .andExpect(status().isOk)
                .andReturn()

        val json = Json.Mapper.readValue<Map<String, Any>>(result.response.contentAsString,
                object : TypeReference<Map<String, Any>>() {

                })
    }

    @Test
    @Throws(Exception::class)
    fun testCountV2() {

        val session = admin()
        addTestAssets("set04/standard")

        val result = mvc.perform(post("/api/v2/assets/_count")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(AssetSearch("beer"))))
                .andExpect(status().isOk)
                .andReturn()

        val counts = Json.Mapper.readValue<Map<String, Any>>(result.response.contentAsString,
                object : TypeReference<Map<String, Any>>() {

                })
        val count = counts["count"] as Int
        assertEquals(1, count.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testSuggestV3() {
        val session = admin()
        val sources = getTestAssets("set04/canyon")
        for (source in sources) {
            source.setAttr("media.keywords", ImmutableList.of("reflection"))
        }
        addTestAssets(sources)

        refreshIndex()

        val result = mvc.perform(post("/api/v3/assets/_suggest")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"text\": \"re\" }".toByteArray()))
                .andExpect(status().isOk)
                .andReturn()

        val json = result.response.contentAsString
        val keywords = Json.Mapper.readValue<List<String>>(json, Json.LIST_OF_STRINGS)

        assertTrue("The list of keywords, '$json' does not contain 'reflection'",
                keywords.contains("reflection"))
    }

    @Test
    @Throws(Exception::class)
    fun testSuggestV3MultipleFields() {
        val session = admin()
        val sources = getTestAssets("set04/canyon")
        for (source in sources) {
            source.setAttr("media.keywords", ImmutableList.of("reflection"))
            source.setAttr("thing.suggest", "resume")
        }
        addTestAssets(sources)
        refreshIndex()

        val result = mvc.perform(post("/api/v3/assets/_suggest")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{ \"text\": \"re\" }".toByteArray()))
                .andExpect(status().isOk)
                .andReturn()

        val json = result.response.contentAsString
        val keywords = Json.Mapper.readValue<List<String>>(json, Json.LIST_OF_STRINGS)

        assertTrue("The list of keywords, '$json' does not contain 'reflection'",
                keywords.contains("reflection"))
        assertTrue("The list of keywords, '$json' does not contain 'resume'",
                keywords.contains("reflection"))
    }

    @Test
    @Throws(Exception::class)
    fun testDelete() {

        val session = admin()
        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexDao.getAll(Pager.first())
        for (asset in assets) {
            val result = mvc.perform(delete("/api/v1/assets/" + asset.id)
                    .session(session)
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk)
                    .andReturn()
            val json = Json.Mapper.readValue<Map<String, Any>>(result.response.contentAsString,
                    object : TypeReference<Map<String, Any>>() {

                    })
            assertEquals(true, json["success"])
            assertEquals("delete", json["op"])
        }
    }

    @Test
    @Throws(Exception::class)
    fun testBatchDelete() {

        val session = admin()
        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexDao.getAll(Pager.first())
        val ids = assets.stream().map { a -> a.id }.toList()

        val result = mvc.perform(delete("/api/v1/assets")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .content(Json.serializeToString(mapOf("assetIds" to ids)))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val rsp = Json.Mapper.readValue(result.response.contentAsString,
                BatchDeleteAssetsResponse::class.java)
        assertEquals(2, rsp.totalRequested)
        assertEquals(2, rsp.deletedAssetIds.size)
    }

    @Test
    @Throws(Exception::class)
    fun testBatchDeleteAccessDenied() {

        val session = user("user")
        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexDao.getAll(Pager.first())
        val ids = assets.stream().map{ a -> a.id }.toList()

        val result = mvc.perform(delete("/api/v1/assets")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .content(Json.serializeToString(mapOf("assetIds" to ids)))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val rsp = Json.Mapper.readValue(result.response.contentAsString,
                BatchDeleteAssetsResponse::class.java)
        assertEquals(0, rsp.totalRequested)
        assertEquals(0, rsp.deletedAssetIds.size)
        assertEquals(0, rsp.onHoldAssetIds.size)
        assertEquals(2, rsp.accessDeniedAssetIds.size)
    }

    @Test
    @Throws(Exception::class)
    fun testGetV2() {

        val session = admin()
        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexDao.getAll(Pager.first())
        for (asset in assets) {
            val result = mvc.perform(get("/api/v2/assets/" + asset.id)
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk)
                    .andReturn()
            val json = Json.Mapper.readValue<Map<String, Any>>(result.response.contentAsString,
                    object : TypeReference<Map<String, Any>>() {

                    })
            assertEquals(asset.id, json["id"])
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetByPath() {

        val session = admin()
        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexDao.getAll(Pager.first())
        for (asset in assets) {
            val url = "/api/v1/assets/_path"
            val result = mvc.perform(get(url)
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serializeToString(ImmutableMap.of("path", asset.getAttr<Any>("source.path")!!))))
                    .andExpect(status().isOk)
                    .andReturn()
            val json = Json.Mapper.readValue<Map<String, Any>>(result.response.contentAsString,
                    object : TypeReference<Map<String, Any>>() {

                    })
            assertEquals(asset.id, json["id"])
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSetFolders() {
        addTestAssets("set04/canyon")

        val folders = Lists.newArrayList<UUID>()
        for (i in 0..9) {
            val builder = FolderSpec("Folder$i")
            val (id) = folderService.create(builder)
            folders.add(id)
        }

        val assets = indexService.getAll(Pager.first(1)).list
        assertEquals(1, assets.size.toLong())
        var doc = assets[0]

        val session = admin()
        mvc.perform(put("/api/v1/assets/" + doc.id + "/_setFolders")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of<String, List<UUID>>("folders", folders))))
                .andExpect(status().isOk)
                .andReturn()

        refreshIndex()
        authenticate("admin")
        doc = indexService.get(doc.id)
        assertEquals(10, doc.getAttr("system.links.folder", List::class.java)!!.size.toLong())

    }

    @Test
    @Throws(Exception::class)
    fun testSetPermissions() {
        addTestAssets("set04/canyon")

        val session = admin()
        val perm = permissionService.getPermission(Groups.ADMIN)
        val req = BatchUpdatePermissionsRequest(AssetSearch(), Acl().addEntry(perm.id, 7))

        mvc.perform(put("/api/v2/assets/_permissions")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(req)))
                .andExpect(status().isOk)
                .andReturn()

        authenticate("admin")
        val assets = indexService.getAll(Pager.first(1))
        for (asset in assets) {
            val perms = asset.getAttr("system.permissions", PermissionSchema::class.java)
            assertTrue(perm.id in perms!!.read)
            assertTrue(perm.id in perms!!.write)
            assertTrue(perm.id in perms!!.export)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFolderAssign() {
        val session = admin()

        addTestAssets("set04/canyon")
        var assets = indexDao.getAll(Pager.first())

        val (id) = folderService.create(FolderSpec("foo"))
        val (id1) = folderService.create(FolderSpec("bar"))
        mvc.run {
            perform(post("/api/v1/folders/$id/assets")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map{ it.id }.collect(Collectors.toList()))))
                .andExpect(status().isOk)
                .andReturn()

            perform(post("/api/v1/folders/$id1/assets")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map{ it.id }.collect(Collectors.toList()))))
                .andExpect(status().isOk)
                .andReturn()
        }

        refreshIndex()
        authenticate("admin")
        assets = indexDao.getAll(Pager.first())
        for (asset in assets) {
            logger.info("{}", asset.document)
            val links = asset.getAttr("system.links.folder", object : TypeReference<List<String>>() {

            })
            assertEquals(2, links.size.toLong())
            assertTrue(
                    links[0] == id.toString() || links[1] == id.toString())
        }
    }

    /**
     * Ignoring until we have good way to test this.
     */
    @Test
    @Ignore
    @Throws(Exception::class)
    fun testStreamHeadRequest() {
        val session = admin()
        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("source.stream", "https://foo/bar")
        indexService.index(source)
        refreshIndex()

        val url = String.format("/api/v1/assets/%s/_stream", source.id)
        mvc.perform(head(url)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andExpect(header().string("X-Zorroa-Signed-URL", "https://foo/bar"))
                .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testStream() {
        val session = admin()
        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexService.getAll(Pager.first())

        val url = String.format("/api/v1/assets/%s/_stream", assets.get(0).id)
        mvc.perform(get(url)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

    }

    /**
     * Bring this back when we support alternative extensions
     */
    @Test
    @Ignore
    @Throws(Exception::class)
    fun testStream404() {
        val session = admin()
        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexService.getAll(Pager.first())

        val url = String.format("/api/v1/assets/%s/_stream?ext=foo", assets.get(0).id)
        val result = mvc.perform(get(url)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError)
                .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testBatchUpdate() {
        addTestAssets("set04/standard")
        refreshIndex()

        authenticate("admin")
        var assets = indexDao.getAll(Pager.first())
        var updates= mutableMapOf<String, UpdateAssetRequest>()

        assets.list.forEach { doc->
            updates[doc.id] = UpdateAssetRequest(mapOf("foos" to "ball"))
        }

        val session = admin()
        val req = BatchUpdateAssetsRequest(updates)
        val result = mvc.perform(put("/api/v1/assets")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .content(Json.serializeToString(req))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val rsp = Json.Mapper.readValue(result.response.contentAsString,
                BatchUpdateAssetsResponse::class.java)
        assertEquals(2, rsp.updatedAssetIds.size)
        assertEquals(0, rsp.erroredAssetIds.size)

        refreshIndex()

        authenticate("admin")
        for (asset in indexDao.getAll(Pager.first())) {
            assertEquals("ball", asset.getAttr("foos", String::class.java))
        }
    }

    @Test
    fun testGetManualEdits() {
        val session = admin()
        addTestAssets("set04/standard")
        var asset = indexDao.getAll(Pager.first())[0]

        val field = fieldSystemService.create(FieldSpec("File Ext",
                "source.extension", null, true))
        assetService.edit(asset.id, FieldEditSpec(field.id,
                "source.extension", "gandalf"))

        val req = mvc.perform(MockMvcRequestBuilders.get("/api/v1/assets/${asset.id}/edits")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = Json.Mapper.readValue<KPagedList<FieldEdit>>(
                req.response.contentAsString, FieldEdit.Companion.TypeRefKList)
        assertEquals(1, result.size())
    }

    @Test
    fun testCreateManualEdit() {
        val session = admin()
        addTestAssets("set04/standard")
        var asset = indexDao.getAll(Pager.first())[0]

        val field = fieldSystemService.create(FieldSpec("File Ext",
                "source.extension", null, true))
        val spec = FieldEditSpec(field.id, null, newValue="bob")

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/assets/${asset.id}/edits")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serializeToString(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = Json.Mapper.readValue<FieldEdit>(req.response.contentAsString, FieldEdit::class.java)
        assertEquals(field.id, result.fieldId)
        assertEquals(UUID.fromString(asset.id), result.assetId)
        assertEquals(spec.newValue, result.newValue)
        assertEquals("jpg", result.oldValue)
    }

    @Test
    fun testDeleteManualEdit() {

        val session = admin()
        addTestAssets("set04/standard")
        var asset = indexDao.getAll(Pager.first())[0]

        val field = fieldSystemService.create(FieldSpec("File Ext",
                "source.extension", null, true))
        val edit = assetService.edit(asset.id, FieldEditSpec(field.id,
                "source.extension", "gandalf"))

        val req = mvc.perform(MockMvcRequestBuilders.delete(
                "/api/v1/assets/${asset.id}/edits/${edit.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serializeToString(edit))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val result = Json.Mapper.readValue<Map<String,Any>>(req.response.contentAsString, Json.GENERIC_MAP)
        assertEquals("update", result["op"])
        assertEquals(true, result["success"])
    }
}
