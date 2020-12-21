package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.AutomlSessionSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.domain.UpdateLabelRequest
import com.zorroa.archivist.service.ModelService
import com.zorroa.archivist.service.PipelineModService
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals

class ModelControllerTests : MockMvcTest() {

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    val modelSpec = ModelSpec("Dog Breeds", ModelType.ZVI_LABEL_DETECTION)

    lateinit var model: Model

    @Before
    fun init() {
        model = modelService.createModel(modelSpec)
    }

    @Test
    fun testCreate() {

        val mspec = ModelSpec(
            "test",
            ModelType.ZVI_LABEL_DETECTION
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mspec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(mspec.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(mspec.name)))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.moduleName",
                    CoreMatchers.equalTo("test")
                )
            )
            .andReturn()
    }

    @Test
    fun testGet() {

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/${model.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(model.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(model.name)))
            .andReturn()
    }

    @Test
    fun testFindOne() {
        val filter =
            """
            {
                "names": ["${model.name}"],
                "ids": ["${model.id}"]
            }
            """
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models/_find_one")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(filter)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(model.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(model.name)))
            .andReturn()
    }

    @Test
    fun testSearch() {

        val filter =
            """
            {
                "names": ["${model.name}"],
                "ids": ["${model.id}"]
            }
            """
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

    @Test
    fun testPublish() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models/${model.id}/_publish")
                .headers(job())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(model.moduleName)))
            .andReturn()
    }

    @Test
    fun testTypeInfo() {
        val type = ModelType.ZVI_LABEL_DETECTION
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/_types/$type")
                .headers(job())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.name",
                    CoreMatchers.equalTo("ZVI_LABEL_DETECTION")
                )
            )
            .andReturn()
    }

    @Test
    fun testGetTypes() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/_types")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$[0].name",
                    CoreMatchers.equalTo("ZVI_KNN_CLASSIFIER")
                )
            )
            .andReturn()
    }

    @Test
    fun testRenameLabel() {
        val specs = dataSet(model)
        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )

        val body = UpdateLabelRequest("ant", "horse")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/models/${model.id}/labels")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(body))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.updated",
                    CoreMatchers.equalTo(1)
                )
            )
            .andReturn()
    }

    @Test
    fun testDeleteLabel() {
        val specs = dataSet(model)
        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )

        val body = UpdateLabelRequest("ant", "")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/models/${model.id}/labels")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(body))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.updated",
                    CoreMatchers.equalTo(1)
                )
            )
            .andReturn()

        authenticate()
        val labels = modelService.getLabelCounts(model)
        assertEquals(null, labels["ant"])
        assertEquals(3, labels.size)
    }

    @Test
    fun testDeleteModel() {
        val specs = dataSet(model)
        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )

        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v3/models/${model.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.success",
                    CoreMatchers.equalTo(true)
                )
            )
            .andReturn()
    }

    @Test
    fun testUploadModel() {
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TF2_IMAGE_CLASSIFIER)
        val model = modelService.createModel(modelSpec)

        val mfp = Paths.get(
            "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models/${model.id}/_upload")
                .headers(admin())
                .content(Files.readAllBytes(mfp))
                .contentType(MediaType.parseMediaType("application/zip"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testSetModelArguments() {
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TF2_IMAGE_CLASSIFIER)
        val model = modelService.createModel(modelSpec)
        modelService.publishModel(model)
        val arg = mapOf("foo" to "bar")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/models/${model.id}/_set_args")
                .headers(admin())
                .content(Json.serialize(arg))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.category",
                    CoreMatchers.equalTo("Custom Models")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.ops[0].apply[0].args.foo",
                    CoreMatchers.equalTo("bar")
                )
            )
            .andReturn()
    }

    @Test
    fun testCreateAutomlSession() {

        val modelSpec = ModelSpec("Dog Breeds 2", ModelType.GCP_LABEL_DETECTION)
        val model = modelService.createModel(modelSpec)

        val automlSpec = AutomlSessionSpec(
            "project/foo/region/us-central/datasets/foo",
            "a_training_job"
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models/${model.id}/_automl")
                .headers(admin())
                .content(Json.serialize(automlSpec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.automlDataSet",
                    CoreMatchers.equalTo(automlSpec.automlDataSet)
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.automlTrainingJob",
                    CoreMatchers.equalTo(automlSpec.automlTrainingJob)
                )
            )
            .andReturn()
    }

    fun dataSet(model: Model): List<AssetSpec> {
        return listOf(
            AssetSpec("https://i.imgur.com/12abc.jpg", label = model.getLabel("beaver")),
            AssetSpec("https://i.imgur.com/abc123.jpg", label = model.getLabel("ant")),
            AssetSpec("https://i.imgur.com/horse.jpg", label = model.getLabel("horse")),
            AssetSpec("https://i.imgur.com/zani.jpg", label = model.getLabel("dog"))
        )
    }
}
