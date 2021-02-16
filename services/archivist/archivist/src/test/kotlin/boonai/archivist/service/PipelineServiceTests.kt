package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Category
import boonai.archivist.domain.ModelObjective
import boonai.archivist.domain.PipelineMod
import boonai.archivist.domain.PipelineModSpec
import boonai.archivist.domain.PipelineMode
import boonai.archivist.domain.PipelineSpec
import boonai.archivist.domain.PipelineUpdate
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.Provider
import boonai.archivist.domain.FileType
import boonai.archivist.security.getProjectId
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Only tests where PipelineService has some business logic.
 */
class PipelineServiceTests : AbstractTest() {

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    val customSpec = PipelineSpec(
        "test", mode = PipelineMode.CUSTOM,
        processors = listOf(
            ProcessorRef("com.zorroa.IngestImages", "image-foo"),
            ProcessorRef("com.zorroa.IngestVideo", "image-foo")
        )
    )

    val modularSpec = PipelineSpec(
        "mod-test",
        mode = PipelineMode.MODULAR,
        // these are ignoed
        processors = listOf(
            ProcessorRef("com.zorroa.IngestImages", "image-foo"),
            ProcessorRef("com.zorroa.IngestVideo", "image-foo")
        )
    )

    @Test
    fun testCreateCustom() {
        val pipeline = pipelineService.create(customSpec)

        assertEquals(customSpec.name, pipeline.name)
        assertEquals(getProjectId(), pipeline.projectId)
        assertTrue(pipeline.modules.isEmpty())
        assertEquals(2, pipeline.processors.size)
    }

    @Test
    fun testCreateModular() {
        modularSpec.modules = listOf(createTestModule("test0").name)
        val pipeline = pipelineService.create(modularSpec)

        assertEquals(modularSpec.name, pipeline.name)
        assertEquals(getProjectId(), pipeline.projectId)
        assertTrue(pipeline.processors.isNullOrEmpty())
        assertEquals(1, pipeline.modules.size)
    }

    @Test
    fun testUpdateCreateCustom() {
        var pipeline = pipelineService.create(customSpec)
        val testMod = createTestModule("test1")

        val updateSpec = PipelineUpdate(
            "cat",
            listOf(ProcessorRef("com.zorroa.IngestImages", "image-foo")),
            listOf(testMod.id)
        )
        assertTrue(pipelineService.update(pipeline.id, updateSpec))
        pipeline = pipelineService.get(pipeline.id)

        assertEquals(updateSpec.name, pipeline.name)
        assertTrue(pipeline.modules.isEmpty())
        assertEquals(1, pipeline.processors.size)
    }

    @Test
    fun testUpdateModular() {
        var pipeline = pipelineService.create(modularSpec)
        val testMod1 = createTestModule("test1")
        val testMod2 = createTestModule("test2")

        val updateSpec = PipelineUpdate(
            "cat",
            listOf(ProcessorRef("com.zorroa.IngestImages", "image-foo")),
            listOf(testMod1.id, testMod2.id)
        )

        assertTrue(pipelineService.update(pipeline.id, updateSpec))
        pipeline = pipelineService.get(pipeline.id)

        assertEquals(updateSpec.name, pipeline.name)
        assertEquals(2, pipeline.modules.size)
        assertTrue(pipeline.processors.isEmpty())
    }

    fun createTestModule(name: String): PipelineMod {
        val modSpec = PipelineModSpec(
            name, "test",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(),
            true
        )
        val mod = pipelineModService.create(modSpec)
        entityManager.flush()
        return mod
    }
}
