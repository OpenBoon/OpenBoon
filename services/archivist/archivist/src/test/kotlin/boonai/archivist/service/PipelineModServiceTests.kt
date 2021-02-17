package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Category
import boonai.archivist.domain.ModOp
import boonai.archivist.domain.ModOpType
import boonai.archivist.domain.ModelObjective
import boonai.archivist.domain.OpFilter
import boonai.archivist.domain.OpFilterType
import boonai.archivist.domain.PipelineMod
import boonai.archivist.domain.PipelineModFilter
import boonai.archivist.domain.PipelineModSpec
import boonai.archivist.domain.PipelineModUpdate
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.Provider
import boonai.archivist.domain.FileType
import boonai.archivist.repository.PipelineModDao
import boonai.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DataRetrievalFailureException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PipelineModServiceTests : AbstractTest() {

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var pipelineModDao: PipelineModDao

    lateinit var mod: PipelineMod

    lateinit var spec: PipelineModSpec

    @Before
    fun init() {
        val op1 = ModOp(
            ModOpType.APPEND,
            listOf(ProcessorRef("foo", "boonai-plugins-foo"))
        )

        val op2 = ModOp(
            ModOpType.SET_ARGS,
            mapOf("extract_pages" to true),
            OpFilter(OpFilterType.SUBSTR, "OfficeImporter")
        )

        spec = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(op1, op2), true
        )
        mod = pipelineModService.create(spec)
    }

    @Test
    fun testCreate() {
        assertTrue(mod.timeModified > 0)
        assertTrue(mod.timeCreated > 0)
        assertEquals(spec.name, mod.name)
        assertEquals(spec.description, mod.description)
        assertEquals(Json.serializeToString(spec.ops), Json.serializeToString(mod.ops))
    }

    @Test(expected = DataIntegrityViolationException::class)
    fun testCreateDuplicateProjectWithStandardName() {
        val spec2 = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(), false
        )
        pipelineModService.create(spec2)
    }

    @Test(expected = DataIntegrityViolationException::class)
    fun testCreateDuplicateProject() {
        val spec1 = PipelineModSpec(
            "test1", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(), false
        )
        val spec2 = PipelineModSpec(
            "test1", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(), false
        )
        pipelineModService.create(spec1)
        pipelineModService.create(spec2)
    }

    @Test(expected = DataIntegrityViolationException::class)
    fun testCreateDuplicateStandard() {
        val spec2 = PipelineModSpec(
            "test", "A test module",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(), true
        )
        pipelineModService.create(spec2)
    }

    @Test
    fun testUpdate() {
        val update = PipelineModUpdate(
            "hodoor",
            "spock",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(ModOp(ModOpType.PREPEND, listOf(ProcessorRef("foo", "boonai-plugins-foo"))))
        )

        Thread.sleep(10)
        val updated = pipelineModService.update(mod.id, update)
        assertEquals(update.description, updated.description)
        assertEquals(update.ops[0].type, updated.ops[0].type)
        assertNotEquals(mod.timeModified, updated.timeModified)

        val ds2 = pipelineModService.get(mod.id)
        assertEquals(update.name, ds2.name)
        assertEquals(update.description, ds2.description)
    }

    @Test
    fun testGet() {
        val module2 = pipelineModService.get(mod.id)
        assertEquals(mod.name, module2.name)
        assertEquals(mod.description, module2.description)
        assertEquals(mod.ops[0].type, module2.ops[0].type)
    }

    @Test
    fun testSearch() {
        val filter = PipelineModFilter(names = listOf(mod.name), ids = listOf(mod.id))
        filter.sort = filter.sortMap.keys.map { "$it:a" }
        val paged = pipelineModService.search(filter)
        assertEquals(1, paged.size())
    }

    @Test
    fun testFindOne() {
        val filter = PipelineModFilter(ids = listOf(mod.id))
        val mod2 = pipelineModService.findOne(filter)
        assertEquals(mod.id, mod2.id)
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testGet_notFound() {
        pipelineModService.get(UUID.randomUUID())
    }

    @Test
    fun testGetByName() {
        val module2 = pipelineModService.getByName(mod.name)
        assertEquals(mod.name, module2.name)
        assertEquals(mod.description, module2.description)
        assertEquals(mod.ops[0].type, module2.ops[0].type)
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testGetByName_notFound() {
        pipelineModService.getByName("kirk")
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testDelete() {
        pipelineModService.delete(mod.id)
        pipelineModService.getByName(mod.name)
    }

    @Test
    fun testUpdateStandardMods() {
        val count = pipelineModDao.count(PipelineModFilter())
        pipelineModService.updateStandardMods()
        assertTrue(pipelineModDao.count(PipelineModFilter()) > count)
    }

    @Test
    fun testGetByNames() {
        pipelineModService.updateStandardMods()
        val names = listOf("boonai-label-detection", "boonai-object-detection")
        val mods = pipelineModService.getByNames(names)
        assertEquals(names.size, mods.size)
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testGetByNames_notFound() {
        pipelineModService.updateStandardMods()
        val names = listOf("boonai-label-detection", "boom!")
        pipelineModService.getByNames(names)
    }

    @Test
    fun testGetByIds() {
        pipelineModService.updateStandardMods()
        val mod = pipelineModService.getByName("boonai-label-detection")
        pipelineModService.getByIds(listOf(mod.id))
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testGetByIds_notFound() {
        pipelineModService.updateStandardMods()
        val ids = listOf(UUID.randomUUID())
        pipelineModService.getByIds(ids)
    }
}
