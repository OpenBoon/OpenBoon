package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Asset
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.JobState
import boonai.archivist.domain.Label
import boonai.archivist.domain.ModOpType
import boonai.archivist.domain.Model
import boonai.archivist.domain.ModelApplyRequest
import boonai.archivist.domain.ModelCopyRequest
import boonai.archivist.domain.ModelFilter
import boonai.archivist.domain.ModelPublishRequest
import boonai.archivist.domain.ModelSpec
import boonai.archivist.domain.ModelTrainingRequest
import boonai.archivist.domain.ModelType
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.ProjectDirLocator
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.UpdateAssetLabelsRequest
import boonai.archivist.security.getProjectId
import boonai.archivist.storage.ProjectStorageService
import boonai.archivist.util.FileUtils
import boonai.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import java.io.FileInputStream
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelServiceTests : AbstractTest() {

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var fileStorageService: ProjectStorageService

    val testSearch =
        """{"query": {"term": { "source.filename": "large-brown-cat.jpg"} } }"""

    fun create(name: String = "test", type: ModelType = ModelType.TF_CLASSIFIER): Model {
        val mspec = ModelSpec(
            name,
            type,
            applySearch = Json.Mapper.readValue(testSearch, Json.GENERIC_MAP)
        )
        return modelService.createModel(mspec)
    }

    @Test
    fun testCreateModel() {
        val model = create()
        assertModel(model)
    }

    @Test
    fun createFaceModelDefaultModuleName() {
        val mspec = ModelSpec(
            "faces",
            ModelType.FACE_RECOGNITION,
            applySearch = Json.Mapper.readValue(testSearch, Json.GENERIC_MAP)
        )
        val model = modelService.createModel(mspec)
        assertEquals("faces", model.moduleName)
    }

    @Test
    fun createFaceModuleCustomModel() {
        val mspec = ModelSpec(
            "faces",
            ModelType.FACE_RECOGNITION,
            moduleName = "foo",
            applySearch = Json.Mapper.readValue(testSearch, Json.GENERIC_MAP)
        )
        val model = modelService.createModel(mspec)
        assertEquals("foo", model.moduleName)
    }

    @Test
    fun testGetModel() {
        val model1 = create()
        val model2 = modelService.getModel(model1.id)
        assertEquals(model2, model2)
        assertModel(model1)
        assertModel(model2)
    }

    @Test
    fun testTrainModel() {
        val model1 = create()
        val job = modelService.trainModel(model1, ModelTrainingRequest())

        assertEquals(model1.trainingJobName, job.name)
        assertEquals(JobState.InProgress, job.state)
        assertEquals(1, job.priority)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testFineOneNotFound() {
        modelService.findOne(ModelFilter(names = listOf("abc")))
    }

    @Test
    fun testFindOne() {
        val model1 = create()
        val filter = ModelFilter(
            names = listOf(model1.name),
            ids = listOf(model1.id),
            types = listOf(model1.type)
        )
        val model2 = modelService.findOne(filter)
        assertEquals(model1, model2)
        assertModel(model2)
    }

    @Test
    fun testFind() {
        val model1 = create()
        val filter = ModelFilter(
            names = listOf(model1.name),
            ids = listOf(model1.id),
            types = listOf(model1.type)
        )
        filter.sort = filter.sortMap.keys.map { "$it:a" }
        val all = modelService.find(filter)
        assertEquals(1, all.size())
        assertEquals(model1, all.list[0])
        assertModel(all.list[0])
    }

    @Test
    fun testPublishModel() {
        val model1 = create()
        val mod = modelService.publishModel(model1, ModelPublishRequest())
        assertEquals(getProjectId(), mod.projectId)
        assertEquals(model1.moduleName, mod.name)
        assertEquals("Boon AI", mod.provider)
        assertEquals("Custom Models", mod.category)
        assertEquals("Label Detection", mod.type)
        assertEquals(ModOpType.APPEND, mod.ops[0].type)
    }

    @Test
    fun testPublishModelWithDepend() {
        val model1 = create(type = ModelType.FACE_RECOGNITION)
        val mod = modelService.publishModel(model1, ModelPublishRequest())
        assertEquals(ModOpType.DEPEND, mod.ops[0].type)
        assertEquals(ModOpType.APPEND, mod.ops[1].type)
        assertEquals(ModelType.FACE_RECOGNITION.dependencies, mod.ops[0].apply as List<String>)
    }

    @Test
    fun testPublishModelUpdate() {
        val model1 = create()
        val mod1 = modelService.publishModel(model1, ModelPublishRequest())
        val mod2 = modelService.publishModel(model1, ModelPublishRequest())

        assertEquals(mod1.id, mod2.id)
        assertEquals(mod1.name, mod2.name)

        val op1 = Json.Mapper.convertValue(mod1.ops[0].apply, ProcessorRef.LIST_OF)
        val op2 = Json.Mapper.convertValue(mod2.ops[0].apply, ProcessorRef.LIST_OF)

        // make sure it versioned up.
        assertNotNull(op1[0].args?.get("version"))
        assertNotNull(op2[0].args?.get("version"))

        assertNotEquals(
            op1[0].args?.get("version"),
            op2[0].args?.get("version")
        )
    }

    @Test
    fun testCopyModel() {
        val model = create(type = ModelType.TF_UPLOADED_CLASSIFIER)
        val mfp = Paths.get(
            "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
        )

        modelService.publishModelFileUpload(model, FileInputStream(mfp.toFile()))
        modelService.copyModelTag(model, ModelCopyRequest("latest", "approved"))

        val files = fileStorageService.listFiles(
            ProjectDirLocator(ProjectStorageEntity.MODELS, model.id.toString()).getPath() + "/approved"
        )

        val names = files.map { FileUtils.filename(it) }
        assertTrue("model-version.txt" in names)
        assertTrue("model.zip" in names)
    }

    @Test
    fun testDeployModelDefaults() {
        setupTestAsset()

        val model1 = create()
        modelService.publishModel(model1, ModelPublishRequest())

        val rsp = modelService.applyModel(model1, ModelApplyRequest())
        val tasks = jobService.getTasks(rsp.job!!.id)
        val script = jobService.getZpsScript(tasks.list[0].id)
        assertEquals("Applying model: test", script.name)
        assertEquals(1, script.generate!!.size)
        assertEquals("boonai_core.core.generators.AssetSearchGenerator", script.generate!![0].className)
    }

    @Test
    fun testDeployModelCustomSearch() {
        setupTestAsset()

        val model1 = create()
        modelService.publishModel(model1, ModelPublishRequest())

        val rsp = modelService.applyModel(
            model1,
            ModelApplyRequest(
                search = Model.matchAllSearch
            )
        )
        val tasks = jobService.getTasks(rsp.job!!.id)
        val script = jobService.getZpsScript(tasks.list[0].id)

        val scriptstr = Json.prettyString(script)
        assertTrue("\"match_all\" : { }" in scriptstr)
        assertTrue("modelId" in scriptstr)
    }

    @Test
    fun excludeLabeledAssetsFromSearch() {
        val model = create()

        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg", label = model.getLabel("husky"))
        val rsp = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val asset = assetService.getAsset(rsp.created[0])
        assetService.index(asset.id, asset.document, true)
        refreshIndex()

        val counts = modelService.getLabelCounts(model)
        assertEquals(1, counts["husky"])

        assertEquals(1, assetSearchService.count(Model.matchAllSearch))
        assertEquals(
            0,
            assetSearchService.count(
                modelService.wrapSearchToExcludeTrainingSet(model, Model.matchAllSearch)
            )
        )
    }

    fun setupTestAsset(): Asset {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            attrs = mapOf("aux.pet" to "dog")
        )
        val req = BatchCreateAssetsRequest(assets = listOf(spec))
        val rsp = assetService.batchCreate(req)
        val asset = assetService.getAsset(rsp.created[0])
        assetService.index(asset.id, asset.document, true)
        refreshIndex()
        return asset
    }

    @Test
    fun getLabelCounts() {
        val model1 = create("test1")
        val model2 = create("test2")

        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(dataSet(model1))
        )

        assetService.updateLabels(
            UpdateAssetLabelsRequest(
                // Validate adding 2 identical labels only adds 1
                mapOf(
                    rsp.created[0] to listOf(
                        Label(model2.id, "house"),
                    ),
                    rsp.created[1] to listOf(
                        Label(model2.id, "tree"),
                    )
                )
            )
        )

        val counts = modelService.getLabelCounts(model1)
        assertEquals(4, counts.size)
        assertEquals(1, counts["ant"])
        assertEquals(1, counts["horse"])
        assertEquals(1, counts["beaver"])
        assertEquals(1, counts["zanzibar"])

        val keys = counts.keys.toList()
        assertEquals("ant", keys[0])
        assertEquals("zanzibar", keys[3])
    }

    @Test
    fun testRenameLabel() {
        val model = create()
        val specs = dataSet(model)

        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )
        modelService.updateLabel(model, "beaver", "horse")
        refreshElastic()
        val counts = modelService.getLabelCounts(model)
        assertEquals(1, counts["ant"])
        assertEquals(2, counts["horse"])
        assertEquals(null, counts["beaver"])
        assertEquals(1, counts["zanzibar"])
    }

    @Test
    fun testRenameLabelToNullForDelete() {
        val model = create()
        val specs = dataSet(model)

        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )
        modelService.updateLabel(model, "horse", null)
        refreshElastic()

        val counts = modelService.getLabelCounts(model)
        assertEquals(1, counts["ant"])
        assertEquals(null, counts["horse"])
        assertEquals(1, counts["beaver"])
        assertEquals(1, counts["zanzibar"])
    }

    @Test
    fun testDeleteModel() {
        val model = create()
        val specs = dataSet(model)

        assetService.batchCreate(BatchCreateAssetsRequest(specs))
        modelService.publishModel(model, ModelPublishRequest())

        assertNotNull(pipelineModService.findByName(model.moduleName, false))

        modelService.deleteModel(model)
        Thread.sleep(2000)

        val counts = modelService.getLabelCounts(model)
        assertTrue(counts.isEmpty())
        assertNull(pipelineModService.findByName(model.moduleName, false))
    }

    @Test
    fun testValidateTModelUpload() {
        modelService.validateTensorflowModel(
            Paths.get(
                "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testValidateModelUploadFail() {
        modelService.validateTensorflowModel(Paths.get("../../../test-data/training/pets.zip"))
    }

    @Test
    fun testAcceptModelFileUploadAndList() {
        val model = create(type = ModelType.TF_UPLOADED_CLASSIFIER)
        val mfp = Paths.get(
            "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
        )

        val module = modelService.publishModelFileUpload(model, FileInputStream(mfp.toFile()))
        assertEquals("Custom Models", module.category)
    }

    @Test
    fun testSetModelArgs() {
        val model = create(type = ModelType.TF_CLASSIFIER)
        modelService.publishModel(model, ModelPublishRequest())

        val module = modelService.setModelArgs(
            model,
            ModelPublishRequest(args = mapOf("input_size" to listOf(321, 321)))
        )
        val str = Json.prettyString(module)
        assertTrue(str.contains("\"input_size\" : [ 321, 321 ]"))
    }

    fun assertModel(model: Model) {
        assertEquals(ModelType.TF_CLASSIFIER, model.type)
        assertEquals("test", model.name)
        assertEquals("test", model.moduleName)
        assertTrue(model.fileId.endsWith("model.zip"))
        assertFalse(model.ready)
    }

    fun dataSet(model: Model): List<AssetSpec> {
        return listOf(
            AssetSpec("https://i.imgur.com/12abc.jpg", label = model.getLabel("beaver")),
            AssetSpec("https://i.imgur.com/abc123.jpg", label = model.getLabel("ant")),
            AssetSpec("https://i.imgur.com/horse.jpg", label = model.getLabel("horse")),
            AssetSpec("https://i.imgur.com/zani.jpg", label = model.getLabel("zanzibar"))
        )
    }

    @Test
    fun testGenerateModelName() {
        val spec = ModelSpec("myModel\n   foo", ModelType.KNN_CLASSIFIER)
        assertEquals("mymodel-foo", modelService.generateModuleName(spec))
    }
}
