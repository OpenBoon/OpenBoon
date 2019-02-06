package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.FieldSpec
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldSystemServiceTests : AbstractTest() {

    @Autowired
    lateinit var fieldSystemService: FieldSystemService

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun createRegular() {
        val spec = FieldSpec("File Path", "source.path", null,false)
        val field = fieldSystemService.create(spec)
        assertEquals("source.path", field.attrName)
        assertEquals("File Path", field.name)
        assertEquals(field.attrType, AttrType.STRING)
        assertEquals(false, field.editable)
    }

    @Test
    fun createCustom() {
        val spec = FieldSpec("Notes", null, AttrType.CONTENT,false)
        val field = fieldSystemService.create(spec)
        assertEquals(AttrType.CONTENT, field.attrType)
        assertTrue(field.custom)
        assertEquals("custom.content__0", field.attrName)
        assertEquals("Notes", field.name)
    }

}