package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.Category
import com.zorroa.archivist.domain.ModOp
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.ModelObjective
import com.zorroa.archivist.domain.OpFilter
import com.zorroa.archivist.domain.OpFilterType
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineFilter
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineMode
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.PipelineUpdate
import com.zorroa.archivist.domain.Provider
import com.zorroa.archivist.domain.FileType
import com.zorroa.archivist.service.PipelineModService
import com.zorroa.zmlp.util.Json
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
            Provider.ZORROA,
            Category.ZORROA_STD,
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
    fun testResolve() {

        mvc.perform(
            get("/api/v1/pipelines/${pl.id}/_resolve")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.execute[0].className",
                    CoreMatchers.equalTo("zmlp_core.core.PreCacheSourceFileProcessor")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.execute[1].args.extract_doc_pages",
                    CoreMatchers.equalTo(true)
                )
            )
            .andReturn()
    }

    @Test
    fun testResolveModular() {
        pipelineModService.updateStandardMods()
        mvc.perform(
            post("/api/v1/pipelines/_resolve_modular")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(listOf("gcp-video-text-detection")))
        )
            .andExpect(status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.execute[0].className",
                    CoreMatchers.equalTo("zmlp_core.core.PreCacheSourceFileProcessor")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.execute[5].className",
                    CoreMatchers.equalTo("zmlp_analysis.google.AsyncVideoIntelligenceProcessor")
                )
            )
            .andReturn()
    }
}
