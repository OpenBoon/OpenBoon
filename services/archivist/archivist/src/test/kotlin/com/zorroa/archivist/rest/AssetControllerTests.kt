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
            ClipSpec(BigDecimal.ONE, BigDecimal.TEN, listOf("cat"), 0.5),
            ClipSpec(BigDecimal("11.2"), BigDecimal("12.5"), listOf("cat"), 0.5)
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

    @Test
    fun testGetWebvttFile() {
        addTestAssets("video")

        val asset = getSample(1)[0]
        val clips = listOf(
            ClipSpec(BigDecimal.ONE, BigDecimal.TEN, listOf("cat"), 0.5),
            ClipSpec(BigDecimal("11.2"), BigDecimal("12.5"), listOf("cat"), 0.5)
        )
        val track = TrackSpec("cats", clips)
        val timeline = TimelineSpec(asset.id, "zvi-label-detection", listOf(track))

        clipService.createClips(timeline)
        refreshElastic()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/${asset.id}/clips/all.vtt")
                .headers(admin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType("text/vtt"))
            .andReturn()
    }

    @Test
    fun testGetWebvttFileByTimeline() {
        addTestAssets("video")

        val asset = getSample(1)[0]
        val clips = listOf(
            ClipSpec(BigDecimal.ONE, BigDecimal.TEN, listOf("cat"), 0.5),
            ClipSpec(BigDecimal("11.2"), BigDecimal("12.5"), listOf("cat"), 0.5)
        )
        val track = TrackSpec("cats", clips)
        val timeline = TimelineSpec(asset.id, "zvi-label-detection", listOf(track))

        clipService.createClips(timeline)
        refreshElastic()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/${asset.id}/clips/timelines/cats.vtt")
                .headers(admin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType("text/vtt"))
            .andReturn()
    }
}
