package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Field
import boonai.archivist.domain.FieldFilter
import boonai.archivist.domain.FieldSpec
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class FieldServiceTests : AbstractTest() {

    @Autowired
    lateinit var fieldService: FieldService

    lateinit var field: Field

    @Before
    fun init() {
        val spec = FieldSpec("player", "text")
        field = fieldService.createField(spec)
    }

    @Test
    fun testCreateField() {
        assertEquals("player", field.name)
        assertEquals("custom.player", field.getPath())
    }

    @Test
    fun testGetField() {
        val field2 = fieldService.getField(field.id)
        assertEquals(field, field2)
    }

    @Test
    fun testFindfieldsEmptyFilter() {
        val filter = FieldFilter()
        val fields = fieldService.findFields(filter)
        assertTrue(fields.list.isNotEmpty())
    }

    @Test
    fun testFindFields() {
        val filter = FieldFilter(
            names = listOf("player"),
            types = listOf("text"),
            ids = listOf(field.id)
        )
        filter.sort = listOf("id:a", "name:a", "type:a")
        val fields = fieldService.findFields(filter)
        assertTrue(fields.list.isNotEmpty())
    }
}
