package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.AssetState
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchDeleteAssetsRequest
import com.zorroa.archivist.domain.ClipSpec
import com.zorroa.archivist.domain.Label
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.domain.TimelineSpec
import com.zorroa.archivist.domain.TrackSpec
import com.zorroa.archivist.domain.UpdateAssetLabelsRequest
import com.zorroa.archivist.service.AssetSearchService
import com.zorroa.archivist.service.ClipService
import com.zorroa.archivist.service.ModelService
import com.zorroa.archivist.service.PipelineModService
import com.zorroa.archivist.util.bbox
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.io.File
import java.math.BigDecimal
import kotlin.test.assertEquals

class AssetControllerTests : MockMvcTest() {

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var clipService: ClipService

    @Test
    fun testBatchCreate() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_batch_create")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("assets" to listOf(spec))))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testDelete() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val id = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec))).created[0]

        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v3/assets/$id")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()
    }

    @Test
    fun testBatchDelete() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val createRsp = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec), state = AssetState.Analyzed))

        val req = BatchDeleteAssetsRequest(createRsp.created.toSet())

        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v3/assets/_batch_delete")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(req))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.deleted[0]", CoreMatchers.equalTo(createRsp.created[0])))
            .andReturn()

        // For co-routing logs
        Thread.sleep(1000)
    }

    @Test
    fun testBatchIndex_withError() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val batch = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = batch.created[0]

        val brokenPayload =
            """
            {
                "$id": {
                    "doc": {
                        "source": {
                            "filename": "cats.png"
                        }
                    },
                    "media": {
                        "type": "image"
                    }
                }
            }
            """

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_batch_index")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(brokenPayload)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.failed.length()", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.failed[0].assetId", CoreMatchers.equalTo(id)))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.failed[0].message",
                    CoreMatchers.containsString("strict_dynamic_mapping_exception")
                )
            )
            .andReturn()
    }

    @Test
    fun testBatchIndex() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val batch = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = batch.created[0]
        val asset = assetService.getAsset(id)
        asset.setAttr("aux.captain", "kirk")
        val payload = mapOf(asset.id to asset.document)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_batch_index")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(payload))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.failed.length()", CoreMatchers.equalTo(0)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.indexed.length()", CoreMatchers.equalTo(1)))
    }

    @Test
    fun testIndex() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = created.created[0]
        val asset = assetService.getAsset(id)

        val rsp = mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/assets/$id/_index")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(asset.document))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$._id", CoreMatchers.equalTo(id)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result", CoreMatchers.equalTo("updated")))
            .andReturn()
    }

    @Test
    fun testUpdate() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = created.created[0]

        val update =
            """
            {
                "doc": {
                    "source": {
                        "filename": "cats.png"
                    }
                }
            }
            """

        val res = mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/$id/_update")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(update)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.result", CoreMatchers.equalTo("updated")))
            .andExpect(MockMvcResultMatchers.jsonPath("$._id", CoreMatchers.equalTo(id)))
            .andReturn()
    }

    @Test
    fun testUpdateByQuery() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))

        val update =
            """
            {
                "query": {
                    "match_all": { }
                },
                "script": {
                    "source": "ctx._source['source']['filename'] = 'test.png'"
                }
            }
            """.trimIndent()

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_update_by_query")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(update)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.updated", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.batches", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.version_conflicts", CoreMatchers.equalTo(0)))
            .andReturn()
    }

    @Test
    fun testBatchUpdateWithDoc() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = created.created[0]

        val update =
            """
            {
                "$id": {
                    "doc": {
                        "source": {
                            "filename": "dogs.png"
                        }
                    }
                }
            }
            """

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_batch_update")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(update)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testUpdateLabels() {
        val ds = modelService.createModel(ModelSpec("test", ModelType.ZVI_KNN_CLASSIFIER))
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))

        val req = UpdateAssetLabelsRequest(
            add = mapOf(created.created[0] to listOf(ds.getLabel("cat")))
        )

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/assets/_batch_update_labels")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(req))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.updated", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errors", CoreMatchers.equalTo(0)))
            .andReturn()
    }

    @Test
    fun testUpdateLabelsWithBbox() {
        val ds = modelService.createModel(ModelSpec("test", ModelType.ZVI_KNN_CLASSIFIER))
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))

        val req = UpdateAssetLabelsRequest(
            add = mapOf(
                created.created[0] to
                    listOf(ds.getLabel("cat", bbox = bbox(0.0, 0.0, 0.1, 0.1)))
            )
        )

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/assets/_batch_update_labels")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(req))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.updated", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errors", CoreMatchers.equalTo(0)))
            .andReturn()

        val req2 = UpdateAssetLabelsRequest(
            add = mapOf(
                created.created[0] to
                    listOf(ds.getLabel("dog", bbox = bbox(0.1, 0.1, 0.3, 0.3)))
            )
        )

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/assets/_batch_update_labels")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(req2))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.updated", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errors", CoreMatchers.equalTo(0)))
            .andReturn()

        authenticate()
        val asset = assetService.getAsset(created.created[0])
        val labels = asset.getAttr("labels", Label.SET_OF)
        assertEquals(2, labels?.size)
    }

    @Test
    fun testGet() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = created.created[0]

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/$id")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", CoreMatchers.equalTo(id)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.document", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testBatchUpload() {

        val file = MockMultipartFile(
            "files", "file-name.data", "image/jpeg",
            File("src/test/resources/test-data/toucan.jpg").inputStream().readBytes()
        )

        val body = MockMultipartFile(
            "body", "",
            "application/json",
            """{"assets":[{"uri": "src/test/resources/test-data/toucan.jpg"}]}""".toByteArray()
        )

        mvc.perform(
            multipart("/api/v3/assets/_batch_upload")
                .file(body)
                .file(file)
                .headers(admin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.created[0]", CoreMatchers.anything()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.failed.length()", CoreMatchers.equalTo(0)))
            .andReturn()
    }

    @Test
    fun testSearchNullBody() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec), state = AssetState.Analyzed))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.hits.total.value", CoreMatchers.equalTo(1)))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.hits.hits[0]._source.source.path",
                    CoreMatchers.equalTo("https://i.imgur.com/SSN26nN.jpg")
                )
            )
            .andReturn()
    }

    @Test
    fun testSearchWithQuerySize() {

        assetService.batchCreate(
            BatchCreateAssetsRequest(
                listOf(
                    AssetSpec("https://i.imgur.com/SSN26nN.jpg"),
                    AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
                ),
                state = AssetState.Analyzed
            )
        )

        val search =
            """{ "size": 1, "query": { "match_all": {}} }"""

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(search)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.hits.total.value", CoreMatchers.equalTo(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.hits.hits.length()", CoreMatchers.equalTo(1)))
            .andReturn()
    }

    @Test
    fun testSearchWithScroll() {

        assetService.batchCreate(
            BatchCreateAssetsRequest(
                listOf(
                    AssetSpec("https://i.imgur.com/SSN26nN.jpg"),
                    AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
                )
            )
        )

        val search =
            """{ "size": 1, "query": { "match_all": {}} }"""

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_search?scroll=1m")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(search)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$._scroll_id", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testScroll() {

        assetService.batchCreate(
            BatchCreateAssetsRequest(
                listOf(
                    AssetSpec("https://i.imgur.com/SSN26nN.jpg"),
                    AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
                )
            )
        )

        val search =
            """{ "size": 1, "query": { "match_all": {}} }"""

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_search?scroll=1m")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(search)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$._scroll_id", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testClearScroll() {
        assetService.batchCreate(
            BatchCreateAssetsRequest(
                listOf(
                    AssetSpec("https://i.imgur.com/SSN26nN.jpg"),
                    AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
                )
            )
        )

        val search = assetSearchService.search(mapOf(), mapOf("scroll" to arrayOf("1m")))
        val body =
            """
            {"scroll_id": "${search.scrollId}" }
            """.trimIndent()
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v3/assets/_search/scroll")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(body)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.succeeded", CoreMatchers.equalTo(true)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.num_freed", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testReprocessSearch() {
        // Create the standard mods which doesn't happen automatically in tests.
        pipelineModService.updateStandardMods()

        assetService.batchCreate(
            BatchCreateAssetsRequest(
                listOf(
                    AssetSpec("https://i.imgur.com/SSN26nN.jpg"),
                    AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
                ),
                state = AssetState.Analyzed
            )
        )

        val search =
            """{ "modules": ["zvi-label-detection"], "search": { "query": { "match_all": { } } } }"""
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_search/reprocess")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(search)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.job.id", CoreMatchers.anything()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.assetCount", CoreMatchers.equalTo(2)))
            .andReturn()
    }

    @Test
    fun testSearchClips() {
        addTestAssets("video")

        val asset = getSample(1)[0]
        val clips = listOf(
            ClipSpec(BigDecimal.ONE, BigDecimal.TEN, "cat", 0.5),
            ClipSpec(BigDecimal("11.2"), BigDecimal("12.5"), "cat", 0.5)
        )
        val track = TrackSpec("cats", clips)
        val timeline = TimelineSpec(asset.id, "zvi-label-detection", listOf(track))

        clipService.createClips(timeline)
        refreshElastic()

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/${asset.id}/clips/_search")
                .headers(admin())
                .content(Json.serialize(timeline))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.hits.total.value", CoreMatchers.equalTo(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.hits.hits.length()", CoreMatchers.equalTo(2)))
            .andReturn()
    }
}
