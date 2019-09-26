package com.zorroa.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.BatchDeleteAssetsResponse
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsResponse
import com.zorroa.archivist.domain.BatchUpdatePermissionsRequest
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Source
import com.zorroa.archivist.domain.UpdateAssetRequest
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.search.Scroll
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.FileServerService
import com.zorroa.archivist.service.FileStat
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.archivist.service.ServableFile
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File
import java.net.URL
import java.util.UUID
import java.util.stream.Collectors
import kotlin.streams.toList

class AssetControllerTests : MockMvcTest() {

    @Autowired
    lateinit var indexDao: IndexDao

    @MockBean
    lateinit var fileStorageService: FileStorageService

    @MockBean
    lateinit var fileServerService: FileServerService

    @SpyBean
    override lateinit var fileServerProvider: FileServerProvider

    @Before
    fun init() {
        setupEmbeddedFieldSets()
    }

    @After
    fun after() {
        SecurityContextHolder.getContext().authentication = null
    }

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    @Throws(Exception::class)
    fun testGetFields() {

        addTestAssets("set04/standard")

        val result = mvc.perform(
            get("/api/v1/assets/_fields")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
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

        addTestAssets("set04/standard")

        val result = mvc.perform(
            post("/api/v3/assets/_search")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(AssetSearch("O'Malley")))
        )
            .andExpect(status().isOk)
            .andReturn()

        val json = Json.Mapper.readValue<Map<String, Any>>(result.response.contentAsString,
            object : TypeReference<Map<String, Any>>() {
            })
    }

    @Test
    fun testClearScroll() {
        addTestAssets("set04/standard")
        val search = AssetSearch("O'Malley")
        search.scroll = Scroll()
        val result = searchService.search(Pager.first(), search)

        val rsp = mvc.perform(
            delete("/api/v1/assets/_scroll")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(mapOf("scroll_id" to result.scroll.id)))
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = Json.Mapper.readValue<Map<String, Any>>(rsp.response.contentAsString)
        assertTrue(body["success"] as Boolean)
    }

    @Test
    @Throws(Exception::class)
    fun testCountV2() {

        addTestAssets("set04/standard")

        val result = mvc.perform(
            post("/api/v2/assets/_count")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(AssetSearch("beer")))
        )
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
    fun testCountWithNullQueryFilter() {

        val filter = null
        assertEquals(5, countWithAssetSearch(filter).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testCountWithQueryFilter() {

        val filter = AssetFilter().setQuery(AssetSearch("hyena"))
        assertEquals(1, countWithAssetSearch(filter).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testCountWithNotQueryFilter() {

        val filter = AssetFilter().setMustNot(mutableListOf(AssetFilter().setQuery(AssetSearch("toucan"))))
        assertEquals(4, countWithAssetSearch(filter).toLong())
    }

    private fun countWithAssetSearch(filter: AssetFilter?): Int {

        addTestAssets("set01")

        val search = AssetSearch("jpg")
        search.filter = filter

        val result = mvc.perform(
            post("/api/v2/assets/_count")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(search))
        )
            .andExpect(status().isOk)
            .andReturn()

        val counts = Json.Mapper.readValue<Map<String, Any>>(result.response.contentAsString)
        return counts["count"] as Int
    }

    @Test
    @Throws(Exception::class)
    fun testSuggestV3() {

        val sources = getTestAssets("set04/canyon")
        for (source in sources) {
            source.setAttr("media.keywords", listOf("reflection"))
        }
        addTestAssets(sources, commitToDb = false)
        refreshIndex()

        val result = mvc.perform(
            post("/api/v3/assets/_suggest")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("text" to "re")))
        )
            .andExpect(status().isOk)
            .andReturn()

        val json = result.response.contentAsString
        val keywords = Json.Mapper.readValue<List<String>>(json, Json.LIST_OF_STRINGS)

        assertTrue(
            "The list of keywords, '$json' does not contain 'reflection'",
            keywords.contains("reflection")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testDelete() {

        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexDao.getAll(Pager.first())
        for (asset in assets) {
            val result = mvc.perform(
                delete("/api/v1/assets/" + asset.id)
                    .headers(admin())
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
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

        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexDao.getAll(Pager.first())
        val ids = assets.stream().map { a -> a.id }.toList()

        val result = mvc.perform(
            delete("/api/v1/assets")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .headers(admin())
                .content(Json.serializeToString(mapOf("assetIds" to ids)))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()

        val rsp = Json.Mapper.readValue(
            result.response.contentAsString,
            BatchDeleteAssetsResponse::class.java
        )
        assertEquals(2, rsp.totalRequested)
        assertEquals(2, rsp.deletedAssetIds.size)
    }

    @Test
    @Throws(Exception::class)
    fun testBatchDeleteAccessDenied() {
        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexDao.getAll(Pager.first())
        val ids = assets.stream().map { a -> a.id }.toList()

        val result = mvc.perform(
            delete("/api/v1/assets")
                .headers(user())
                .content(Json.serializeToString(mapOf("assetIds" to ids)))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()

        val rsp = Json.Mapper.readValue(
            result.response.contentAsString,
            BatchDeleteAssetsResponse::class.java
        )
        assertEquals(0, rsp.totalRequested)
        assertEquals(0, rsp.deletedAssetIds.size)
        assertEquals(0, rsp.onHoldAssetIds.size)
        assertEquals(2, rsp.accessDeniedAssetIds.size)
    }

    @Test
    @Throws(Exception::class)
    fun testGetV2() {

        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexDao.getAll(Pager.first())
        for (asset in assets) {
            val result = mvc.perform(
                get("/api/v2/assets/" + asset.id)
                    .headers(admin())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
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

        addTestAssets("set04/standard")
        refreshIndex()

        val assets = indexDao.getAll(Pager.first())
        for (asset in assets) {
            val url = "/api/v1/assets/_path"
            val result = mvc.perform(
                get(url)
                    .headers(admin())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serializeToString(ImmutableMap.of("path", asset.getAttr<Any>("source.path")!!)))
            )
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

        mvc.perform(
            put("/api/v1/assets/" + doc.id + "/_setFolders")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of<String, List<UUID>>("folders", folders)))
        )
            .andExpect(status().isOk)
            .andReturn()

        refreshIndex()
        authenticate("admin")
        doc = indexService.get(doc.id)
        println(Json.prettyString(doc))
        assertEquals(10, doc.getAttr("system.links.folder", List::class.java)!!.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testSetPermissions() {
        addTestAssets("set04/canyon")

        val perm = permissionService.getPermission(Groups.ADMIN)
        val req = BatchUpdatePermissionsRequest(AssetSearch(), Acl().addEntry(perm.id, 7))

        mvc.perform(
            put("/api/v2/assets/_permissions")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(req))
        )
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

        addTestAssets("set04/canyon")
        var assets = indexDao.getAll(Pager.first())

        val (id) = folderService.create(FolderSpec("foo"))
        val (id1) = folderService.create(FolderSpec("bar"))
        mvc.run {
            perform(
                post("/api/v1/folders/$id/assets")
                    .headers(admin())
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(assets.stream().map { it.id }.collect(Collectors.toList())))
            )
                .andExpect(status().isOk)
                .andReturn()

            perform(
                post("/api/v1/folders/$id1/assets")
                    .headers(admin())
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(assets.stream().map { it.id }.collect(Collectors.toList())))
            )
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
                links[0] == id.toString() || links[1] == id.toString()
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun testStreamHeadRequest() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        indexService.index(source)

        val uri = source.path.toUri()
        val servableFile = ServableFile(fileServerService, uri)

        val anyDocument = object : Document() {
            override fun equals(other: Any?): Boolean = true
        }
        doReturn(servableFile).`when`(fileServerProvider).getServableFile(anyDocument)

        given(fileServerService.storedLocally).willReturn(false)
        given(fileServerService.objectExists(uri)).willReturn(true)

        val signedUrl = "https://signed/url"
        given(fileServerService.getSignedUrl(uri)).willReturn(URL(signedUrl))

        val url = String.format("/api/v1/assets/%s/_stream", source.id)
        mvc.perform(
            head(url)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.header().string("X-Zorroa-Signed-URL", signedUrl))
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testStreamSource() {

        val source = Source(File("src/test/resources/test-data/toucan.jpg"))
        indexService.index(source)

        val url = String.format("/api/v1/assets/%s/_stream", source.id)
        mvc.perform(
            get(url)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType("image/jpeg"))
            .andReturn()
    }

    @Test
    fun testStreamProxy() {

        val asset = videoAsset()
        val mediaType = "video/mp4"
        val tmpFile = createTempFile()
        val tmpFileUri = tmpFile.toURI()
        val fileStorage = FileStorage(asset.id, tmpFileUri, "file", mediaType, fileServerProvider)

        doReturn(ServableFile(fileServerService, tmpFileUri)).`when`(fileServerProvider).getServableFile(tmpFileUri)

        given(fileStorageService.get("proxy___${asset.getAttr<String>("proxy_id")}_transcode.mp4"))
            .willReturn(fileStorage)
        given(fileServerService.storedLocally).willReturn(true)
        given(fileServerService.getLocalPath(tmpFileUri)).willReturn(tmpFile.toPath())
        given(fileServerService.getStat(tmpFileUri)).willReturn(FileStat(0, mediaType, true))

        val url = String.format("/api/v1/assets/%s/_stream", asset.id)

        mvc.perform(
            get(url)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(mediaType))
            .andReturn()

        tmpFile.delete()
    }

    @Test
    fun testStreamHeadWithAcceptHeaderNoValidOptions() {

        val asset = videoAsset()
        val mediaType = "video/mov"
        val tmpFile = createTempFile()
        val tmpFileUri = tmpFile.toURI()
        val fileStorage = FileStorage(asset.id, tmpFileUri, "file", mediaType, fileServerProvider)

        doReturn(ServableFile(fileServerService, tmpFileUri)).`when`(fileServerProvider).getServableFile(tmpFileUri)

        given(fileStorageService.get("proxy___${asset.getAttr<String>("proxy_id")}_transcode.mp4"))
            .willReturn(fileStorage)
        given(fileServerService.storedLocally).willReturn(true)

        val url = String.format("/api/v1/assets/%s/_stream", asset.id)
        val accept = "video/webm,video/ogg"
        mvc.perform(
            head(url)
                .headers(admin())
                .header("Accept", accept)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isNotFound)
            .andExpect(MockMvcResultMatchers.header().doesNotExist("X-Zorroa-Signed-URL"))
            .andReturn()

        tmpFile.delete()
    }

    @Test
    fun testStreamHeadWithAcceptHeaderForProxy() {

        val asset = videoAsset()
        val mediaType = "video/mov"
        val tmpFile = createTempFile()
        val tmpFileUri = tmpFile.toURI()
        val fileStorage = FileStorage(asset.id, tmpFileUri, "file", mediaType, fileServerProvider)

        doReturn(ServableFile(fileServerService, tmpFileUri)).`when`(fileServerProvider).getServableFile(tmpFileUri)

        given(fileStorageService.get("proxy___${asset.getAttr<String>("proxy_id")}_transcode.mp4"))
            .willReturn(fileStorage)
        given(fileServerService.storedLocally).willReturn(true)

        val url = String.format("/api/v1/assets/%s/_stream", asset.id)
        val accept = "video/mp4,video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5"
        mvc.perform(
            head(url)
                .headers(admin())
                .header("Accept", accept)
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.header().doesNotExist("X-Zorroa-Signed-URL"))
            .andReturn()

        tmpFile.delete()
    }

    @Test
    @Throws(Exception::class)
    fun testBatchUpdate() {
        addTestAssets("set04/standard")
        refreshIndex()

        authenticate("admin")
        var assets = indexDao.getAll(Pager.first())
        var updates = mutableMapOf<String, UpdateAssetRequest>()

        assets.list.forEach { doc ->
            updates[doc.id] = UpdateAssetRequest(mapOf("foos" to "ball"))
        }

        val req = BatchUpdateAssetsRequest(updates)
        val result = mvc.perform(
            put("/api/v1/assets")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .headers(admin())
                .content(Json.serializeToString(req))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()

        val rsp = Json.Mapper.readValue(
            result.response.contentAsString,
            BatchUpdateAssetsResponse::class.java
        )
        assertEquals(2, rsp.updatedAssetIds.size)
        assertEquals(0, rsp.erroredAssetIds.size)

        refreshIndex()

        authenticate("admin")
        for (asset in indexDao.getAll(Pager.first())) {
            assertEquals("ball", asset.getAttr("foos", String::class.java))
        }
    }

    @Test
    fun testGetFieldSets() {

        addTestAssets("set04/standard")
        var asset = indexDao.getAll(Pager.first())[0]

        logger.info(Json.prettyString(fieldSystemService.getAllFieldSets()))

        val field = fieldSystemService.getField("media.title")
        val spec = FieldEditSpec(UUID.fromString(asset.id), field.id, null, newValue = "The Hobbit 2")
        assetService.createFieldEdit(spec)

        val req = mvc.perform(
            MockMvcRequestBuilders.get(
                "/api/v1/assets/${asset.id}/fieldSets"
            )
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val result = Json.Mapper.readValue<Any>(req.response.contentAsString, Json.LIST_OF_GENERIC_MAP)
        println(Json.prettyString(result))
    }

    private fun videoAsset(): Document {
        addTestVideoAssets()
        val assets = indexService.getAll(Pager.first())
        return assets.get(0)
    }
}
