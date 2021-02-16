package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.Category
import boonai.archivist.domain.OpFilter
import boonai.archivist.domain.OpFilterType
import boonai.archivist.domain.ModOp
import boonai.archivist.domain.ModOpType
import boonai.archivist.domain.ModelObjective
import boonai.archivist.domain.PipelineModSpec
import boonai.archivist.domain.PipelineModUpdate
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.Provider
import boonai.archivist.service.PipelineModService
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

class PipelineModControllerTests : MockMvcTest() {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var pipelineModService: PipelineModService

    val spec = PipelineModSpec(
        "test", "A test module",
        Provider.BOONAI,
        Category.BOONAI_STD,
        ModelObjective.LABEL_DETECTION,
        listOf(),
        listOf(
            ModOp(
                ModOpType.SET_ARGS,
                mapOf("extract_pages" to true),
                OpFilter(OpFilterType.SUBSTR, "OfficeImporter")
            )
        )
    )

    @Test
    fun testCreate() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/pipeline-mods")
                .headers(admin())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(spec.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.description", CoreMatchers.equalTo(spec.description)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.ops[0].type", CoreMatchers.equalTo("SET_ARGS")))
            .andReturn()
    }

    @Test
    fun testUpdate() {

        val op = ModOp(
            ModOpType.APPEND,
            listOf(ProcessorRef("foo", "bar"))
        )

        val mod = pipelineModService.create(spec)
        val update = PipelineModUpdate(
            name = "cats", description = "dogs",
            provider = Provider.BOONAI,
            category = Category.BOONAI_STD,
            type = ModelObjective.CLIPIFIER,
            supportedMedia = listOf(), ops = listOf(op)
        )

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/pipeline-mods/${mod.id}")
                .headers(admin())
                .content(Json.serialize(update))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("cats")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.description", CoreMatchers.equalTo("dogs")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo("Video Clip Generator")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.ops[0].type", CoreMatchers.equalTo("APPEND")))
            .andReturn()
    }

    @Test
    fun testGet() {
        val mod = pipelineModService.create(spec)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/pipeline-mods/${mod.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(spec.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.description", CoreMatchers.equalTo(spec.description)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.ops[0].type", CoreMatchers.equalTo("SET_ARGS")))
            .andReturn()
    }

    @Test
    fun testDelete() {
        val mod = pipelineModService.create(spec)
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/pipeline-mods/${mod.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testSearchNoBody() {
        pipelineModService.create(spec)
        entityManager.flush()
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/pipeline-mods/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.list.length()", CoreMatchers.equalTo(1)))
            .andReturn()
    }

    @Test
    fun testSearchWithFilter() {
        pipelineModService.create(spec)
        entityManager.flush()

        val filter =
            """
            {
                "names": ["arg!"]
            }
            """.trimIndent()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/pipeline-mods/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(filter)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.list.length()", CoreMatchers.equalTo(0)))
    }

    @Test
    fun testFindOne_404() {
        pipelineModService.create(spec)
        entityManager.flush()

        val filter =
            """
            {
                "names": ["arg!"]
            }
            """.trimIndent()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/pipeline-mods/_find_one")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(filter)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    fun testFindOne() {
        pipelineModService.create(spec)
        entityManager.flush()

        val filter =
            """
            {
                "names": ["test"]
            }
            """

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/pipeline-mods/_find_one")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(filter)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("test")))
    }
}
