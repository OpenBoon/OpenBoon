package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.DataSet
import com.zorroa.archivist.domain.DataSetSpec
import com.zorroa.archivist.domain.DataSetType
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.service.DataSetService
import com.zorroa.archivist.service.ModelService
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class ModelControllerTests : MockMvcTest() {

    @Autowired
    lateinit var dataSetService: DataSetService

    @Autowired
    lateinit var modelService: ModelService

    val dsSpec = DataSetSpec("dog-breeds", DataSetType.LabelDetection)

    lateinit var dataSet: DataSet

    @Before
    fun init() {
        dataSet = dataSetService.create(dsSpec)
    }

    fun createTestModel(): Model {
        val mspec = ModelSpec(
            dataSet.id,
            ModelType.TF2_XFER_MOBILENET2
        )
        return modelService.createModel(mspec)
    }

    @Test
    fun testCreate() {

        val mspec = ModelSpec(
            dataSet.id,
            ModelType.TF2_XFER_MOBILENET2
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mspec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(mspec.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.dataSetId", CoreMatchers.equalTo(mspec.dataSetId.toString())))
            .andReturn()
    }

    @Test
    fun testGet() {
        val model = createTestModel()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/${model.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(model.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.dataSetId", CoreMatchers.equalTo(model.dataSetId.toString())))
            .andReturn()
    }

    @Test
    fun testFindOne() {
        val model = createTestModel()
        val filter = """{
            "names": ["${model.name}"],
            "ids": ["${model.id}"]
        }""".trimIndent()
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models/_find_one")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(filter)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(model.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.dataSetId", CoreMatchers.equalTo(model.dataSetId.toString())))
            .andReturn()
    }

    @Test
    fun testSearch() {
        val model = createTestModel()
        val filter = """{
            "names": ["${model.name}"],
            "ids": ["${model.id}"]
        }""".trimIndent()
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(filter)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].type", CoreMatchers.equalTo(model.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].name", CoreMatchers.equalTo(model.name)))
            .andReturn()
    }

    @Test
    fun testTrain() {
        val model = createTestModel()
        val body = mapOf<String, Any>()
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models/${model.id}/_train")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(body))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(model.trainingJobName)))
            .andReturn()
    }
}
