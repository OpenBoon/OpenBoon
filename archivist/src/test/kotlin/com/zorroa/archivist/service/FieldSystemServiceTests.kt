package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.FieldSpec
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldSystemServiceTests : AbstractTest() {

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun createRegular() {
        val spec = FieldSpec("File Extension", "source.extension", null,false)
        val field = fieldSystemService.createField(spec)
        assertEquals("source.extension", field.attrName)
        assertEquals("File Extension", field.name)
        assertEquals(field.attrType, AttrType.String)
        assertEquals(false, field.editable)
    }

    @Test
    fun createCustom() {
        val spec = FieldSpec("Notes", null, AttrType.StringContent,false)
        val field = fieldSystemService.createField(spec)
        assertEquals(AttrType.StringContent, field.attrType)
        assertTrue(field.custom)
        assertEquals("custom.stringcontent__0", field.attrName)
        assertEquals("Notes", field.name)
    }

}