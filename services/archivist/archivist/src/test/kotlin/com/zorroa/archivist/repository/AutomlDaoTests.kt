package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AutomlSession
import com.zorroa.archivist.domain.AutomlSessionSpec
import com.zorroa.archivist.domain.AutomlSessionState
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.service.AutomlService
import com.zorroa.archivist.service.ModelService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals

class AutomlDaoTests : AbstractTest() {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var automlDao: AutomlDao

    @Autowired
    lateinit var automlService: AutomlService

    @Autowired
    lateinit var modelService: ModelService

    @Test
    fun testSetError() {
        val session = create()
        assertEquals(1, automlDao.setError("foo bar", session.id))
        // entityManager.clear()

        val session2 = automlDao.getOne(session.id)
        assertEquals("foo bar", session2.error)
        assertEquals(AutomlSessionState.ERROR, session2.state)
    }

    @Test
    fun testSetFinished() {
        val session = create()
        // Can only be finished once.
        assertEquals(1, automlDao.setFinished("/foo/bar", session.id))
        assertEquals(0, automlDao.setFinished("/foo/bar", session.id))

        val session2 = automlDao.getOne(session.id)
        assertEquals("/foo/bar", session2.automlModel)
        assertEquals(AutomlSessionState.FINISHED, session2.state)
    }

    fun create(): AutomlSession {
        val modelSpec = ModelSpec("animals", ModelType.GCP_LABEL_DETECTION)
        val model = modelService.createModel(modelSpec)

        val automlSpec = AutomlSessionSpec(
            "project/foo/region/us-central/datasets/foo",
            "a_training_job"
        )

        return automlService.createSession(model, automlSpec)
    }
}
