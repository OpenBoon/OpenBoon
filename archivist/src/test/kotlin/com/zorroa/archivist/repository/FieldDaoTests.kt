package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.FieldFilter
import com.zorroa.archivist.domain.FieldSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import org.junit.Assert.*

class FieldDaoTests : AbstractTest() {

    @Autowired
    lateinit var fieldDao: FieldDao

    @Test
    fun testCreate() {
        val spec = FieldSpec("Notes", "document.notes", AttrType.StringAnalyzed, false)
        val field = fieldDao.create(spec)
        assertEquals(spec.name, field.name)
        assertEquals(spec.attrType, field.attrType)
        assertEquals(spec.attrName, field.attrName)
        assertEquals(spec.custom, field.custom)
        assertEquals(spec.editable, field.editable)
    }

    @Test
    fun testDelete() {
        val spec = FieldSpec("Notes", "document.notes", AttrType.StringAnalyzed, false)
        val field = fieldDao.create(spec)
        assertTrue(fieldDao.delete(field))
        assertFalse(fieldDao.delete(field))
    }

    @Test
    fun testGet() {
        val spec = FieldSpec("Notes", "document.notes", AttrType.StringAnalyzed, false)
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

        val f1 = fieldDao.create(FieldSpec("Notes", "document.notes", AttrType.StringAnalyzed, false))
        val f2 = fieldDao.create(FieldSpec("Boats", "document.number", AttrType.NumberInteger, false))
        val f3 = fieldDao.create(FieldSpec("Moats", "document.float", AttrType.NumberFloat, false))

        var filter = FieldFilter(ids = listOf(f1.id, f2.id))
        assertEquals(2, fieldDao.getAll(filter).size())

        filter = FieldFilter(attrTypes=listOf(AttrType.NumberInteger))
        assertEquals(1, fieldDao.getAll(filter).size())

        filter = FieldFilter(attrNames=listOf("document.float", "document.notes"))
        assertEquals(2, fieldDao.getAll(filter).size())
    }

    @Test
    fun testAllocate() {
        var field = fieldDao.allocate(AttrType.StringAnalyzed)
        println(field)
        assertTrue(field.endsWith("__0"))

        field = fieldDao.allocate(AttrType.StringAnalyzed)
        assertTrue(field.endsWith("__1"))

        field = fieldDao.allocate(AttrType.NumberInteger)
        assertTrue(field.endsWith("__0"))
    }

}
