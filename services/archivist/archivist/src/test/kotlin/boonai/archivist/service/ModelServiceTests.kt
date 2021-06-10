package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Asset
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.DataSet
import boonai.archivist.domain.DataSetSpec
import boonai.archivist.domain.DataSetType
import boonai.archivist.domain.JobState
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
    lateinit var modelDeployService: ModelDeployService

    @Autowired
    lateinit var dataSetService: DataSetService

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var fileStorageService: ProjectStorageService

    val testSearch =
        """{"query": {"term": { "source.filename": "large-brown-cat.jpg"} } }"""

    fun create(name: String = "test", type: ModelType = ModelType.TF_CLASSIFIER, ds: DataSet? = null): Model {
        val mspec = ModelSpec(
            name,
            type,
            dataSetId = ds?.id,
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
        val ds = dataSetService.createDataSet(DataSetSpec("frogs", DataSetType.Classification))
        val model1 = create(ds = ds)
        val job = modelService.trainModel(model1, ModelTrainingRequest())

        assertEquals(model1.trainingJobName, job.name)
        assertEquals(JobState.InProgress, job.state)
        assertEquals(100, job.priority)
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
        val model = create(type = ModelType.TF_SAVED_MODEL)
        val mfp = Paths.get(
            "../../../test-data/training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip"
        )

        modelDeployService.deployUploadedModel(model, FileInputStream(mfp.toFile()))
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
        val ds = dataSetService.createDataSet(DataSetSpec("cats", DataSetType.Classification))
        val model1 = create(ds = ds)
        modelService.publishModel(model1, ModelPublishRequest())

        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT int_model_count FROM dataset WHERE pk_dataset=?", Int::class.java, ds.id
            )
        )

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
        assertTrue("dataSetId" in scriptstr)
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
    fun testDeleteModel() {
        val model = create()
        modelService.deleteModel(model)
        assertNull(pipelineModService.findByName(model.moduleName, false))
    }

    @Test
    fun testSetTrainingArgs() {
        val model = create(type = ModelType.TF_CLASSIFIER)
        modelService.publishModel(model, ModelPublishRequest())

        modelService.setTrainingArgs(
            model,
            mapOf("epochs" to 20)
        )
    }

    fun assertModel(model: Model) {
        assertEquals(ModelType.TF_CLASSIFIER, model.type)
        assertEquals("test", model.name)
        assertEquals("test", model.moduleName)
        assertTrue(model.fileId.endsWith("model.zip"))
        assertFalse(model.ready)
    }

    @Test
    fun testGenerateModelName() {
        val spec = ModelSpec("myModel\n   foo", ModelType.KNN_CLASSIFIER)
        assertEquals("mymodel-foo", modelService.generateModuleName(spec))
    }
}
