package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import org.junit.Assert.*

class FieldDaoTests : AbstractTest() {

    @Autowired
    internal var fieldDao: FieldDao? = null

    @Test
    fun testAddIgnoreField() {
        assertTrue(fieldDao!!.hideField("foo.bar.bing", true))
        fieldDao!!.hideField("foo.bar.bing", true)
        assertEquals(1, (jdbc.queryForObject("SELECT COUNT(1) FROM field_hide", Int::class.java) as Int).toLong())
    }

    @Test
    fun testUnignoreField() {
        assertTrue(fieldDao!!.hideField("foo.bar.bing", true))
        assertTrue(fieldDao!!.unhideField("foo.bar.bing"))
        assertFalse(fieldDao!!.unhideField("foo.bar.bing"))
    }
}
