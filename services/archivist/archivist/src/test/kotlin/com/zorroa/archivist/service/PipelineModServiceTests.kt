package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.OpFilter
import com.zorroa.archivist.domain.OpFilterType
import com.zorroa.archivist.domain.ModOp
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModFilter
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineModUpdate
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.repository.PipelineModDao
import com.zorroa.zmlp.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataRetrievalFailureException
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
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

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Before
    fun init() {
        val op1 = ModOp(
            ModOpType.APPEND,
            listOf(ProcessorRef("foo", "zmlp-plugins-foo"))
        )

        val op2 = ModOp(
            ModOpType.SET_ARGS,
            mapOf("extract_pages" to true),
            OpFilter(OpFilterType.SUBSTR, "OfficeImporter")
        )

        spec = PipelineModSpec("test", "A test module", listOf(op1, op2))
        mod = pipelineModService.create(spec)
    }

    @Test
    fun testCreate() {
        assertTrue(mod.timeModified > 0)
        assertTrue(mod.timeCreated > 0)
        assertEquals(spec.name, mod.name)
        assertEquals(spec.restricted, mod.restricted)
        assertEquals(spec.description, mod.description)
        assertEquals(Json.serializeToString(spec.ops), Json.serializeToString(mod.ops))
    }

    @Test
    fun testUpdate() {
        val update = PipelineModUpdate(
            "hodoor",
            "spock",
            true,
            listOf(ModOp(ModOpType.PREPEND, listOf(ProcessorRef("foo", "zmlp-plugins-foo"))))
        )
        Thread.sleep(10)
        val updated = pipelineModService.update(mod.id, update)
        assertEquals(update.name, updated.name)
        assertEquals(update.restricted, updated.restricted)
        assertEquals(update.description, updated.description)
        assertEquals(Json.serializeToString(update.ops ?: ""), Json.serializeToString(updated.ops))
        assertNotEquals(mod.timeModified, updated.timeModified)

        val ds2 = pipelineModService.get(mod.id)
        assertEquals(update.name, ds2.name)
        assertEquals(update.description, ds2.description)
    }

    @Test
    fun testGet() {
        val module2 = pipelineModService.get(mod.id)
        assertEquals(mod.name, module2.name)
        assertEquals(mod.restricted, module2.restricted)
        assertEquals(mod.description, module2.description)
        assertEquals(Json.serializeToString(mod.ops), Json.serializeToString(module2.ops))
    }

    @Test
    fun testSearch() {
        entityManager.flush()
        val filter = PipelineModFilter(names = listOf(mod.name), ids = listOf(mod.id))
        filter.sort = filter.sortMap.keys.map { "$it:a" }
        val paged = pipelineModService.search(filter)
        assertEquals(1, paged.size())
    }

    @Test
    fun testFindOne() {
        entityManager.flush()
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
        val module2 = pipelineModService.get(mod.name)
        assertEquals(mod.name, module2.name)
        assertEquals(mod.restricted, module2.restricted)
        assertEquals(mod.description, module2.description)
        assertEquals(Json.serializeToString(mod.ops), Json.serializeToString(module2.ops))
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testGetByName_notFound() {
        pipelineModService.get("kirk")
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testDelete() {
        pipelineModService.delete(mod.id)
        entityManager.flush()
        pipelineModService.get(mod.name)
    }

    @Test
    fun testUpdateStandardMods() {
        val count = pipelineModDao.count()
        pipelineModService.updateStandardMods()
        assertTrue(pipelineModDao.count() > count)
    }

    @Test
    fun testGetByNames() {
        pipelineModService.updateStandardMods()
        val names = listOf("zvi-label-detection", "zvi-object-detection")
        val mods = pipelineModService.getByNames(names)
        assertEquals(names.size, mods.size)
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testGetByNames_notFound() {
        pipelineModService.updateStandardMods()
        val names = listOf("zvi-label-detection", "boom!")
        pipelineModService.getByNames(names)
    }

    @Test
    fun testGetByIds() {
        pipelineModService.updateStandardMods()
        val mod = pipelineModService.get("zvi-label-detection")
        pipelineModService.getByIds(listOf(mod.id))
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testGetByIds_notFound() {
        pipelineModService.updateStandardMods()
        val ids = listOf(UUID.randomUUID())
        pipelineModService.getByIds(ids)
    }
}
