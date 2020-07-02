package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.DataSet
import com.zorroa.archivist.domain.DataSetFilter
import com.zorroa.archivist.domain.DataSetLabel
import com.zorroa.archivist.domain.DataSetSpec
import com.zorroa.archivist.domain.DataSetType
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelApplyRequest
import com.zorroa.archivist.domain.ModelFilter
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelTrainingArgs
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.UpdateAssetLabelsRequest
import com.zorroa.archivist.security.getProjectId
import com.zorroa.zmlp.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelServiceTests : AbstractTest() {

    @Autowired
    lateinit var dataSetService: DataSetService

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    val dsSpec = DataSetSpec("dog-breeds", DataSetType.LABEL_DETECTION)
    lateinit var dataSet: DataSet

    val testSearch =
        """{"query": {"term": { "source.filename": "large-brown-cat.jpg"} } }"""

    fun create(): Model {
        dataSet = dataSetService.create(dsSpec)
        val mspec = ModelSpec(
            dataSet.id,
            ModelType.ZVI_LABEL_DETECTION,
            Json.Mapper.readValue(testSearch, Json.GENERIC_MAP)
        )
        return modelService.createModel(mspec)
    }

    @Test
    fun testCreateModel() {
        val model = create()
        assertModel(model)
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
        val job = modelService.trainModel(model1, ModelTrainingArgs())
        dataSetService.get(model1.dataSetId).modified

        assertEquals(false, dataSetService.get(model1.dataSetId).modified)
        assertEquals(model1.trainingJobName, job.name)
        assertEquals(JobState.InProgress, job.state)
        assertEquals(1, job.priority)
    }

    @Test
    fun testUpdateDataSetModified() {
        val model1 = create()

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/cat-movie.m4v"))
        )
        // Add a label.
        var asset = assetService.getAsset(assetService.batchCreate(batchCreate).created[0])
        assetService.updateLabels(
            UpdateAssetLabelsRequest(
                mapOf(
                    asset.id to listOf(
                        DataSetLabel(dataSet.id, "cat", simhash = "12345")
                    )
                )
            )
        )

        // Must be set to true when a label is added
        var modifiedDatSet = dataSetService.findOne(DataSetFilter(listOf(dataSet.id)))

        assertEquals(true, modifiedDatSet.modified)

        val job = modelService.trainModel(model1, ModelTrainingArgs())

        modifiedDatSet = dataSetService.findOne(DataSetFilter(listOf(dataSet.id)))

        // Must be set to false when a model is retrained
        assertEquals(false, modifiedDatSet.modified)
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
            dataSetIds = listOf(model1.dataSetId),
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
            dataSetIds = listOf(model1.dataSetId),
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
        val mod = modelService.publishModel(model1)
        Json.prettyPrint(mod)
        assertEquals(getProjectId(), mod.projectId)
        assertEquals(model1.name, mod.name)
        assertEquals("Custom", mod.provider)
        assertEquals("Custom Model", mod.category)
        assertEquals("Label Detection", mod.type)
        assertEquals(ModOpType.APPEND, mod.ops[0].type)
    }

    @Test
    fun testPublishModelUpdate() {
        val model1 = create()
        val mod1 = modelService.publishModel(model1)
        val mod2 = modelService.publishModel(model1)

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
    fun testDeployModelDefaults() {
        setupTestAsset()

        val model1 = create()
        modelService.publishModel(model1)
        val job = modelService.deployModel(model1, ModelApplyRequest())
        val tasks = jobService.getTasks(job.jobId)
        val script = jobService.getZpsScript(tasks.list[0].id)
        assertEquals("Deploying model zvi-dog-breeds-label-detection", script.name)
        assertEquals(1, script.generate!!.size)
        assertEquals("zmlp_core.core.generators.AssetSearchGenerator", script.generate!![0].className)
    }

    @Test
    fun testDeployModelCustomSearch() {
        setupTestAsset()

        val model1 = create()
        modelService.publishModel(model1)

        val job = modelService.deployModel(model1, ModelApplyRequest(search = Model.matchAllSearch))
        val tasks = jobService.getTasks(job.jobId)
        val script = jobService.getZpsScript(tasks.list[0].id)

        val scriptstr = Json.prettyString(script)
        assertTrue("\"match_all\" : { }" in scriptstr)
        assertTrue("dataSetId" in scriptstr)
    }

    @Test
    fun testDeployModelCustomSearchDisableLabelFilter() {
        setupTestAsset()

        val model1 = create()
        modelService.publishModel(model1)

        val job = modelService.deployModel(
            model1,
            ModelApplyRequest(
                search = Model.matchAllSearch,
                excludeTrainingSet = false
            )
        )
        val tasks = jobService.getTasks(job.jobId)
        val script = jobService.getZpsScript(tasks.list[0].id)

        val scriptstr = Json.prettyString(script)
        assert("dataSetId" !in scriptstr)
    }

    @Test
    fun excludeLabeledAssetsFromSearch() {
        val model = create()

        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg", label = dataSet.getLabel("husky"))
        val rsp = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val asset = assetService.getAsset(rsp.created[0])
        assetService.index(asset.id, asset.document, true)
        refreshIndex()

        val counts = dataSetService.getLabelCounts(dataSet)
        assertEquals(1, counts["husky"])

        assertEquals(1, assetSearchService.count(Model.matchAllSearch))
        assertEquals(
            0,
            assetSearchService.count(
                modelService.wrapSearchToExcludeLabels(model, Model.matchAllSearch)
            )
        )
    }

    fun setupTestAsset(): Asset {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            mapOf("aux.pet" to "dog")
        )
        val req = BatchCreateAssetsRequest(assets = listOf(spec))
        val rsp = assetService.batchCreate(req)
        val asset = assetService.getAsset(rsp.created[0])
        assetService.index(asset.id, asset.document, true)
        refreshIndex()
        return asset
    }

    fun assertModel(model: Model) {
        assertEquals(ModelType.ZVI_LABEL_DETECTION, model.type)
        assertEquals("zvi-dog-breeds-label-detection", model.name)
        assertTrue(model.fileId.endsWith("zvi-dog-breeds-label-detection.zip"))
        assertFalse(model.ready)
    }
}
