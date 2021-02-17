package boonai.common.service.jpa

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConvertersTests {

    private val stringListConverter = StringListConverter()

    @Test
    fun stringListConverterEmptyStringEntityAttr() {
        val result = stringListConverter.convertToEntityAttribute("")
        assertEquals(0, result?.size)
    }

    @Test
    fun stringListConverterNullStringEntityAttr() {
        val result = stringListConverter.convertToEntityAttribute(null)
        assertTrue(result?.isEmpty() ?: false)
    }

    @Test
    fun stringListConverterEntityAttr() {
        val result = stringListConverter.convertToEntityAttribute("cat,dog")
        assertEquals(2, result?.size)
        assertEquals(2, result?.size)
    }

    @Test
    fun stringListConverterNullListDbCol() {
        val result = stringListConverter.convertToDatabaseColumn(null)
        assertNull(result)
    }

    @Test
    fun stringListConverterDbColEmptyList() {
        val result = stringListConverter.convertToDatabaseColumn(listOf())
        assertEquals("", result)
    }

    @Test
    fun stringListConverterDbCol() {
        val result = stringListConverter.convertToDatabaseColumn(listOf("cat", "dog"))
        assertEquals("cat,dog", result)
    }
}
