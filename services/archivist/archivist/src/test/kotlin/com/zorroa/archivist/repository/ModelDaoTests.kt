package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.service.ModelService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelDaoTests : AbstractTest() {

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var modelJdbcDao: ModelJdbcDao

    @Test
    fun testMarkAsReady() {
        val model = modelService.createModel(ModelSpec("foo", ModelType.ZVI_LABEL_DETECTION))
        modelJdbcDao.markAsReady(model.id, true)
        var trained = jdbc.queryForObject(
            "SELECT bool_trained FROM model WHERE pk_model=?", Boolean::class.java, model.id
        )
        assertTrue(trained)

        modelJdbcDao.markAsReady(model.id, false)
        trained = jdbc.queryForObject(
            "SELECT bool_trained FROM model WHERE pk_model=?", Boolean::class.java, model.id
        )
        assertFalse(trained)
    }
}
