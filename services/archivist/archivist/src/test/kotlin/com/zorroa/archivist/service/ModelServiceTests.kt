package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.DataSet
import com.zorroa.archivist.domain.DataSetSpec
import com.zorroa.archivist.domain.DataSetType
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.JobType
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelFilter
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelTrainingArgs
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.security.getProjectId
import com.zorroa.zmlp.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelServiceTests : AbstractTest() {

    @Autowired
    lateinit var dataSetService: DataSetService

    @Autowired
    lateinit var modelService: ModelService

    val dsSpec = DataSetSpec("dog-breeds", DataSetType.LabelDetection)
    lateinit var dataSet: DataSet

    fun create(): Model {
        dataSet = dataSetService.create(dsSpec)
        val mspec = ModelSpec(
            dataSet.id,
            ModelType.TF2_XFER_MOBILENET2
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

        assertEquals(model1.trainingJobName, job.name)
        assertEquals(JobType.Batch, job.type)
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
            dataSetIds = listOf(model1.dataSetId),
            ids = listOf(model1.id),
            types = listOf(model1.type))
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
            types = listOf(model1.type))
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

    fun assertModel(model: Model) {
        assertEquals(ModelType.TF2_XFER_MOBILENET2, model.type)
        assertEquals("custom-dog-breeds-label-detection-mobilenet2", model.name)
        assertTrue(model.fileId.endsWith("custom-dog-breeds-label-detection-mobilenet2.zip"))
        assertFalse(model.ready)
    }
}
