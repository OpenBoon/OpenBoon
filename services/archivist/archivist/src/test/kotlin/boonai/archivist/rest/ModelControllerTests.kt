package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.domain.Model
import boonai.archivist.domain.ModelApplyRequest
import boonai.archivist.domain.ModelPatchRequest
import boonai.archivist.domain.ModelPatchRequestV2
import boonai.archivist.domain.ModelSpec
import boonai.archivist.domain.ModelType
import boonai.archivist.domain.ModelUpdateRequest
import boonai.archivist.service.DatasetService
import boonai.archivist.service.ModelDeployService
import boonai.archivist.service.ModelService
import boonai.archivist.service.PipelineModService
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ModelControllerTests : MockMvcTest() {

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var modelDeployServic: ModelDeployService

    @Autowired
    lateinit var datasetService: DatasetService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    val modelSpec = ModelSpec("Dog Breeds", ModelType.TF_CLASSIFIER)

    lateinit var model: Model

    @Before
    fun init() {
        model = modelService.createModel(modelSpec)
    }

    @Test
    fun testCreate() {

        val mspec = ModelSpec(
            "test",
            ModelType.TF_CLASSIFIER
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
        entityManager.flush()
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
        entityManager.flush()
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
        val ds = datasetService.createDataset(DatasetSpec("bob", DatasetType.Classification))
        modelService.patchModel(model.id, ModelPatchRequestV2().apply { this.setDatasetId(ds.id) })

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
    fun testTest() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models/${model.id}/_test")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ModelApplyRequest()))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
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
        val type = ModelType.TF_CLASSIFIER
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/_types/$type")
                .headers(job())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.name",
                    CoreMatchers.equalTo("TF_CLASSIFIER")
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
                    CoreMatchers.equalTo("KNN_CLASSIFIER")
                )
            )
            .andReturn()
    }

    @Test
    fun testDeleteModel() {
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
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TORCH_MAR_CLASSIFIER)
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
    fun testApproveLatestModelTag() {
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TORCH_MAR_CLASSIFIER)
        val model = modelService.createModel(modelSpec)
        val mfp = Paths.get(
            "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
        )
        modelDeployServic.deployUploadedModel(model, FileInputStream(mfp.toFile()))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models/${model.id}/_approve")
                .headers(admin())
                .content(Files.readAllBytes(mfp))
                .contentType(MediaType.parseMediaType("application/zip"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.success",
                    CoreMatchers.equalTo(true)
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.type",
                    CoreMatchers.equalTo("model")
                )
            )
            .andReturn()
    }

    @Test
    fun testSetModelTrainingArguments() {
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TF_CLASSIFIER)
        val model = modelService.createModel(modelSpec)
        val args = mapOf("epochs" to 20)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/models/${model.id}/_training_args")
                .headers(admin())
                .content(Json.serialize(args))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.epochs",
                    CoreMatchers.equalTo(20)
                )
            )
            .andReturn()
    }

    @Test
    fun testPatchModelTrainingArguments() {
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TF_CLASSIFIER)
        val model = modelService.createModel(modelSpec)
        val args = mapOf("epochs" to 20)

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v3/models/${model.id}/_training_args")
                .headers(admin())
                .content(Json.serialize(args))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.epochs",
                    CoreMatchers.equalTo(20)
                )
            )
            .andReturn()
    }

    @Test
    fun testPatchModelInvalidTrainingArguments() {
        val modelSpec = ModelSpec("Dolphin Breeds", ModelType.TORCH_MAR_IMAGE_SEGMENTER)
        val model = modelService.createModel(modelSpec)
        val args = mapOf(
            "labels" to "Animal",
            "colors" to "Black"
        )

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v3/models/${model.id}/_training_args")
                .headers(admin())
                .content(Json.serialize(args))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
    }

    @Test
    fun testGetModelArgSchema() {
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TF_CLASSIFIER)
        val model = modelService.createModel(modelSpec)
        val args = mapOf("epochs" to 20)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/_types/${model.type}/_training_args")
                .headers(admin())
                .content(Json.serialize(args))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.args.epochs.type",
                    CoreMatchers.equalTo("Integer")
                )
            )
            .andReturn()
    }

    @Test
    fun testGetResolvedArgs() {
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TF_CLASSIFIER)
        val model = modelService.createModel(modelSpec)
        val args = mapOf("epochs" to 20)

        modelService.setTrainingArgs(model, args)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/${model.id}/_training_args")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.epochs",
                    CoreMatchers.equalTo(20)
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.validation_split",
                    CoreMatchers.equalTo(0.2)
                )
            )
            .andReturn()
    }

    @Test
    fun testGetType() {
        val module = ModelType.TF_CLASSIFIER
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/_types/${module.name}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.name",
                    CoreMatchers.equalTo(module.name)
                )
            ).andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.label",
                    CoreMatchers.equalTo(module.label)
                )
            )
            .andReturn()
    }

    @Test
    fun testGetTags() {
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TORCH_MAR_CLASSIFIER)
        val model = modelService.createModel(modelSpec)

        val mfp = Paths.get(
            "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
        )
        modelDeployServic.deployUploadedModel(model, FileInputStream(mfp.toFile()))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/${model.id}/_tags")
                .headers(admin())
                .content(Files.readAllBytes(mfp))
                .contentType(MediaType.parseMediaType("application/zip"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$[0]",
                    CoreMatchers.equalTo("latest")
                )
            )
            .andReturn()
    }

    @Test
    fun testPatch() {
        val ds = datasetService.createDataset(DatasetSpec("stuff", DatasetType.Classification))
        val patch = ModelPatchRequest(name = "mongo", datasetId = ds.datasetId())

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v3/models/${model.id}")
                .headers(admin())
                .content(Json.serialize(patch))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()
    }

    @Test
    fun testPatchUnsetDataset() {
        val ds = datasetService.createDataset(DatasetSpec("stuff", DatasetType.Classification))
        val model = modelService.createModel(ModelSpec("foo", ModelType.KNN_CLASSIFIER, datasetId = ds.id))

        val req = """
            {"name": "mongo", "datasetId": null, "dependencies": ["boonai-text-detection"] }
        """.trimMargin()

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v3/models/${model.id}")
                .headers(admin())
                .content(req)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()

        assertNull(model.datasetId)
        assertEquals(model.dependencies, listOf("boonai-text-detection"))
    }

    @Test
    fun testPatchNoDataset() {
        val ds = datasetService.createDataset(DatasetSpec("stuff", DatasetType.Classification))
        val model = modelService.createModel(ModelSpec("foo", ModelType.KNN_CLASSIFIER, datasetId = ds.id))

        val req = """
            {"name": "mongo"}
        """.trimMargin()

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v3/models/${model.id}")
                .headers(admin())
                .content(req)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()

        assertNotNull(model.datasetId)
    }

    @Test
    fun testUpdate() {
        val ds = datasetService.createDataset(DatasetSpec("stuff", DatasetType.Classification))
        val update = ModelUpdateRequest(name = "mongo", datasetId = ds.datasetId(), dependencies = listOf())

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/models/${model.id}")
                .headers(admin())
                .content(Json.serialize(update))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()
    }

    @Test
    fun getSignedUploadUrl() {
        val model = modelService.createModel(ModelSpec("foo", ModelType.TORCH_MAR_CLASSIFIER))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/${model.id}/_get_upload_url")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uri", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun deployModel() {
        val model = modelService.createModel(ModelSpec("foo", ModelType.TORCH_MAR_CLASSIFIER))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/models/${model.id}/_deploy")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()
    }
}
