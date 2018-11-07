package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException

import org.junit.Assert.assertEquals

/**
 * Created by chambers on 5/30/17.
 */
class SettingsDaoTests : AbstractTest() {

    @Autowired
    internal var settingsDao: SettingsDao? = null

    @After
    fun after() {
        System.clearProperty("foo.bar")
    }

    @Test
    fun testSet() {
        settingsDao!!.set("foo.bar", 1)
        assertEquals("1", settingsDao!!.get("foo.bar"))
    }

    @Test
    fun testGetAll() {
        settingsDao!!.set("foo.bar", 1)
        val all = settingsDao!!.getAll()
        assertEquals("1", all["foo.bar"])
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testUnset() {
        settingsDao!!.set("foo.bar", 1)
        assertEquals("1", settingsDao!!.get("foo.bar"))
        settingsDao!!.unset("foo.bar")
        settingsDao!!.get("foo.bar")
    }


}
