package com.zorroa.archivist.web

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Source
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.web.api.AssetController
import com.zorroa.common.util.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*
import java.util.stream.Collectors

class AssetControllerTests : MockMvcTest() {

    @Autowired
    lateinit var indexDao: IndexDao

    @Autowired
    lateinit var assetController: AssetController

    @Before
    fun init() {
        fieldService.invalidateFields()
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
    fun testHideAndUnhideField() {
        val session = admin()
        addTestAssets("set04/standard")

        val result = mvc.perform(put("/api/v1/assets/_fields/hide")
                .session(session)
                .content(Json.serializeToString(ImmutableMap.of("field", "source.")))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        var status = Json.Mapper.readValue<Map<String, Any>>(result.response.contentAsString,
                object : TypeReference<Map<String, Any>>() {

                })
        assertTrue(status["success"] as Boolean)

        authenticate("admin")
        fieldService.invalidateFields()
        val fields = fieldService.getFields("asset")
        for (field in fields["string"]!!) {
            assertFalse(field.startsWith("source"))
        }

        mvc.perform(delete("/api/v1/assets/_fields/hide")
                .session(session)
                .content(Json.serializeToString(ImmutableMap.of("field", "source.")))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        status = Json.Mapper.readValue(result.response.contentAsString,
                object : TypeReference<Map<String, Any>>() {

                })
        assertTrue(status["success"] as Boolean)

        authenticate("admin")
        val stringFields = fieldService.getFields("asset")
        assertNotEquals(fields, stringFields)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchV3() {

        val session = admin()
        addTestAssets("set04/standard")

        val result = mvc.perform(post("/api/v3/assets/_search")
                .session(session)
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
        authenticate("admin")
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
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of<String, List<UUID>>("folders", folders))))
                .andExpect(status().isOk)
                .andReturn()

        refreshIndex()
        authenticate("admin")
        doc = indexService.get(doc.id)
        assertEquals(10, doc.getAttr("zorroa.links.folder", List::class.java).size.toLong())

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
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(assets.stream().map{ it.id }.collect(Collectors.toList()))))
                .andExpect(status().isOk)
                .andReturn()

            perform(post("/api/v1/folders/$id1/assets")
                .session(session)
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
            val links = asset.getAttr("zorroa.links.folder", object : TypeReference<List<String>>() {

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
}
