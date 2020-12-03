package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.FieldSpec
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class FieldServiceTests : AbstractTest() {

    @Autowired
    lateinit var fieldService: FieldService

    @Test
    fun testCreateField() {
        val spec = FieldSpec("player", "text")
        val field = fieldService.createField(spec)
        assertEquals("player", field.name)
        assertEquals("custom.player", field.getEsField())
    }

    @Test
    fun testGetField() {
        val spec = FieldSpec("player", "text")
        val field1 = fieldService.createField(spec)
        val field2 = fieldService.getField(field1.id)
        assertEquals(field1, field2)
    }
}
