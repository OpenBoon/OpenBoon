package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.BuildZpsScriptRequest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.Category
import boonai.archivist.domain.ModOp
import boonai.archivist.domain.ModOpType
import boonai.archivist.domain.ModelObjective
import boonai.archivist.domain.OpFilter
import boonai.archivist.domain.OpFilterType
import boonai.archivist.domain.Pipeline
import boonai.archivist.domain.PipelineFilter
import boonai.archivist.domain.PipelineModSpec
import boonai.archivist.domain.PipelineMode
import boonai.archivist.domain.PipelineSpec
import boonai.archivist.domain.PipelineUpdate
import boonai.archivist.domain.Provider
import boonai.archivist.domain.FileType
import boonai.archivist.service.PipelineModService
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

class PipelineControllerTests : MockMvcTest() {

    lateinit var pl: Pipeline
    lateinit var spec: PipelineSpec

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Before
    fun init() {
        val modSpec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("extract_doc_pages" to true),
                    OpFilter(OpFilterType.SUBSTR, "FileImportProcessor")
                )
            )
        )
        val mod = pipelineModService.create(modSpec)
        entityManager.flush()

        spec = PipelineSpec("Zorroa Test", modules = listOf(mod.name))
        pl = pipelineService.create(spec)
    }

    @Test
    fun testCreate() {

        val spec = PipelineSpec("ZorroaTest2", processors = listOf())
        mvc.perform(
            post("/api/v1/pipelines")
                .headers(admin())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.name",
                    CoreMatchers.equalTo(spec.name)
                )
            )
            .andReturn()
    }

    @Test
    fun testDelete() {

        mvc.perform(
            delete("/api/v1/pipelines/" + pl.id)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.success",
                    CoreMatchers.equalTo(true)
                )
            )
            .andReturn()
    }

    @Test
    fun testUpdate() {
        val spec2 = PipelineUpdate(
            "Rocky IV",
            pl.processors,
            pl.modules
        )

        mvc.perform(
            put("/api/v1/pipelines/" + pl.id)
                .headers(admin())
                .content(Json.serialize(spec2))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.success",
                    CoreMatchers.equalTo(true)
                )
            )
            .andReturn()
    }

    @Test
    fun testGet() {

        mvc.perform(
            get("/api/v1/pipelines/" + pl.id)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.id",
                    CoreMatchers.equalTo(pl.id.toString())
                )
            )
            .andReturn()
    }

    @Test
    fun testOneFindByName() {
        val find =
            """
            {"names": ["Zorroa Test"]}
            """.trimIndent()
        mvc.perform(
            get("/api/v1/pipelines/_findOne")
                .headers(admin())
                .content(find)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.name",
                    CoreMatchers.equalTo(spec.name)
                )
            )
            .andReturn()
    }

    @Test
    fun testSearchSorted() {
        // sort by everything
        val filter = PipelineFilter()
        filter.sort = filter.sortMap.keys.map { "$it:asc" }

        mvc.perform(
            get("/api/v1/pipelines/_search")
                .headers(admin())
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.list[0].name",
                    CoreMatchers.equalTo("default")
                )
            )
            .andReturn()
    }

    @Test
    fun testSearchByNameAndType() {
        // sort by everything
        val filter = PipelineFilter(names = listOf("default"), modes = listOf(PipelineMode.MODULAR))
        mvc.perform(
            get("/api/v1/pipelines/_search")
                .headers(admin())
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.list[0].name",
                    CoreMatchers.equalTo("default")
                )
            )
            .andReturn()
    }

    @Test
    fun testSearchById() {
        // sort by everything
        val filter = PipelineFilter(ids = listOf(pl.id))
        mvc.perform(
            get("/api/v1/pipelines/_search")
                .headers(admin())
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.list[0].name",
                    CoreMatchers.equalTo("Zorroa Test")
                )
            )
            .andReturn()
    }

    @Test
    fun testResolveApplyModulesToAsset() {
        pipelineModService.updateStandardMods()
        val spec = AssetSpec("gs://cats/large-brown-cat.jpg")
        val create = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )

        refreshElastic()
        var asset = assetService.getAsset(assetService.batchCreate(create).created[0])
        val req = BuildZpsScriptRequest(
            asset.id,
            modules = listOf("boonai-face-detection")
        )

        mvc.perform(
            post("/api/v3/pipelines/resolver/_build_script")
                .headers(admin())
                .content(Json.serialize(req))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.execute[0].className",
                    CoreMatchers.equalTo("boonai_analysis.boonai.ZviFaceDetectionProcessor")
                )
            )
            .andReturn()
    }
}
