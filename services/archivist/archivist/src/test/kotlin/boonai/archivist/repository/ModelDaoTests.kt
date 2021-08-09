package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.ModelFilter
import boonai.archivist.domain.ModelSpec
import boonai.archivist.domain.ModelState
import boonai.archivist.domain.ModelType
import boonai.archivist.service.ModelService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class ModelDaoTests : AbstractTest() {

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var modelJdbcDao: ModelJdbcDao

    @Test
    fun setEndpoint() {
        val model = modelService.createModel(ModelSpec("foo", ModelType.TF_CLASSIFIER))
        modelJdbcDao.setEndpoint(model.id, "https://foo/bar")

        var endpoint = jdbc.queryForObject(
            "SELECT str_endpoint FROM model WHERE pk_model=?", String::class.java, model.id
        )
        assertEquals(endpoint, "https://foo/bar")
    }

    @Test
    fun testUpdateState() {
        val model1 = modelService.createModel(ModelSpec("test1", ModelType.TF_CLASSIFIER))
        for (state in ModelState.values()) {
            modelJdbcDao.updateState(model1.id, state)
        }
    }

    @Test
    fun testFindOrderDesc() {
        val model1 = modelService.createModel(ModelSpec("test1", ModelType.TF_CLASSIFIER))
        Thread.sleep(100)
        val model2 = modelService.createModel(ModelSpec("test2", ModelType.TF_CLASSIFIER))
        Thread.sleep(100)
        val model3 = modelService.createModel(ModelSpec("test3", ModelType.TF_CLASSIFIER))

        val find = modelJdbcDao.find(ModelFilter())

        assertEquals(model3.id, find[0].id)
        assertEquals(model2.id, find[1].id)
        assertEquals(model1.id, find[2].id)
    }
}
