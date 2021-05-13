package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.AutomlSessionSpec
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.Model
import boonai.archivist.domain.ModelApplyRequest
import boonai.archivist.domain.ModelSpec
import boonai.archivist.domain.ModelType
import boonai.archivist.domain.UpdateLabelRequest
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
import kotlin.test.assertEquals

class ModelControllerTests : MockMvcTest() {

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var pipelineModService: PipelineModService

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
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TF_UPLOADED_CLASSIFIER)
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
    fun testDownloadModel() {
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TF_UPLOADED_CLASSIFIER)
        val model = modelService.createModel(modelSpec)

        val mfp = Paths.get(
            "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
        )

        modelService.publishModelFileUpload(model, Files.readAllBytes(mfp).inputStream())

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/models/${model.id}/_download")
                .headers(admin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers
                    .content()
                    .contentType(MediaType.parseMediaType("application/zip"))
            )
            .andReturn()
    }

    @Test
    fun testApproveLatestModelTag() {
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TF_UPLOADED_CLASSIFIER)
        val model = modelService.createModel(modelSpec)
        val mfp = Paths.get(
            "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
        )
        modelService.publishModelFileUpload(model, FileInputStream(mfp.toFile()))

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
    fun testCreateAutomlSession() {

        val modelSpec = ModelSpec("Dog Breeds 2", ModelType.GCP_AUTOML_CLASSIFIER)
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
        val modelSpec = ModelSpec("Dog Breeds2", ModelType.TF_UPLOADED_CLASSIFIER)
        val model = modelService.createModel(modelSpec)

        val mfp = Paths.get(
            "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
        )
        modelService.publishModelFileUpload(model, FileInputStream(mfp.toFile()))

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

    fun dataSet(model: Model): List<AssetSpec> {
        return listOf(
            AssetSpec("https://i.imgur.com/12abc.jpg", label = model.getLabel("beaver")),
            AssetSpec("https://i.imgur.com/abc123.jpg", label = model.getLabel("ant")),
            AssetSpec("https://i.imgur.com/horse.jpg", label = model.getLabel("horse")),
            AssetSpec("https://i.imgur.com/zani.jpg", label = model.getLabel("dog"))
        )
    }
}
