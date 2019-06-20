package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.FieldFilter
import com.zorroa.archivist.domain.FieldSpec
import com.zorroa.archivist.domain.FieldUpdateSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectResultSizeDataAccessException

class FieldDaoTests : AbstractTest() {

    @Autowired
    lateinit var fieldDao: FieldDao

    @Test
    fun testCreate() {
        val spec = FieldSpec("Notes", "document.notes", AttrType.StringAnalyzed)

        val field = fieldDao.create(spec)
        assertEquals(spec.name, field.name)
        assertEquals(spec.attrType, field.attrType)
        assertEquals(spec.attrName, field.attrName)
        assertEquals(false, field.custom)
        assertEquals(spec.editable, field.editable)
        assertEquals(spec.keywords, field.keywords)
        assertEquals(spec.keywordsBoost, field.keywordsBoost)
        assertEquals(spec.suggest, field.suggest)
    }

    @Test
    fun testDelete() {
        val spec = FieldSpec("Notes", "document.notes", AttrType.StringAnalyzed)
        val field = fieldDao.create(spec)
        assertTrue(fieldDao.delete(field))
        assertFalse(fieldDao.delete(field))
    }

    @Test
    fun testUpdate() {
        val spec = FieldSpec("Notes", "document.notes", AttrType.StringAnalyzed)
        val field = fieldDao.create(spec)

        val updateSpec = FieldUpdateSpec(
            "test", true, true, 2.0f, true, requireList = true
        )

        assertTrue(fieldDao.update(field, updateSpec))
        val result = fieldDao.get(field.id)

        assertEquals(updateSpec.name, result.name)
        assertEquals(updateSpec.editable, result.editable)
        assertEquals(updateSpec.keywords, result.keywords)
        assertEquals(updateSpec.keywordsBoost, result.keywordsBoost)
        assertEquals(updateSpec.suggest, result.suggest)
    }

    @Test
    fun testGet() {
        val spec = FieldSpec("Notes", "document.notes", AttrType.StringAnalyzed)
        val field1 = fieldDao.create(spec)
        val field2 = fieldDao.get(field1.id)
        assertEquals(field1.id, field2.id)
        assertEquals(field1.name, field2.name)
        assertEquals(field1.attrType, field2.attrType)
        assertEquals(field1.attrName, field2.attrName)
        assertEquals(field1.custom, field2.custom)
        assertEquals(field1.editable, field2.editable)
    }

    @Test
    fun testGetAll() {
        // Clear out existing fields to make filters easier.
        fieldDao.deleteAll()

        val f1 = fieldDao.create(FieldSpec("Notes", "document.notes", AttrType.StringAnalyzed)
            .apply { suggest = true })
        val f2 = fieldDao.create(FieldSpec("Boats", "document.number", AttrType.NumberInteger))
        val f3 = fieldDao.create(FieldSpec("Moats", "document.float", AttrType.NumberFloat)
            .apply { editable = true })

        var filter = FieldFilter(ids = listOf(f1.id, f2.id))
        assertEquals(2, fieldDao.getAll(filter).size())

        filter = FieldFilter(attrTypes = listOf(AttrType.NumberInteger))
        assertEquals(1, fieldDao.getAll(filter).size())

        filter = FieldFilter(attrNames = listOf("document.float", "document.notes"))
        assertEquals(2, fieldDao.getAll(filter).size())

        filter = FieldFilter(editable = true, attrNames = listOf("document.float"))
        assertEquals(1, fieldDao.getAll(filter).size())

        filter = FieldFilter(editable = false)
        assertEquals(2, fieldDao.getAll(filter).size())

        filter = FieldFilter(editable = false)
        assertEquals(2, fieldDao.getAll(filter).size())

        filter = FieldFilter(suggest = true)
        assertEquals(1, fieldDao.getAll(filter).size())
    }

    @Test(expected = IncorrectResultSizeDataAccessException::class)
    fun testFindOne() {
        // Clear out existing fields to make filters easier.
        fieldDao.deleteAll()

        val f1 = fieldDao.create(FieldSpec("Notes", "document.notes", AttrType.StringAnalyzed))
        val f2 = fieldDao.create(FieldSpec("Boats", "document.number", AttrType.NumberInteger))

        var filter = FieldFilter(ids = listOf(f1.id))
        val result1 = fieldDao.findOne(filter)
        assertEquals(f1.id, result1.id)

        fieldDao.findOne(FieldFilter(ids = listOf(f1.id, f2.id)))
    }

    @Test
    fun testAllocate() {
        var field = fieldDao.allocate(AttrType.StringAnalyzed)
        assertTrue(field.endsWith("__0"))

        field = fieldDao.allocate(AttrType.StringAnalyzed)
        assertTrue(field.endsWith("__1"))

        field = fieldDao.allocate(AttrType.NumberInteger)
        assertTrue(field.endsWith("__0"))
    }

    @Test
    fun testGetKeywordAttrNames() {
        setupEmbeddedFieldSets()
        var fields = fieldDao.getKeywordAttrNames()
        assertTrue(fields.isNotEmpty())
        for ((field, _) in fields) {
            assertFalse(field.endsWith(".raw"))
        }

        var rawFields = fieldDao.getKeywordAttrNames(forExactMatch = true)
        assertTrue(rawFields.isNotEmpty())
        for ((field, _) in rawFields) {
            assertTrue(field.endsWith(".raw"))
        }
    }
}
