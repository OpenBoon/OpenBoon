package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.AutomlSession
import boonai.archivist.domain.AutomlSessionSpec
import boonai.archivist.domain.AutomlSessionState
import boonai.archivist.domain.ModelSpec
import boonai.archivist.domain.ModelType
import boonai.archivist.service.AutomlService
import boonai.archivist.service.ModelService
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
