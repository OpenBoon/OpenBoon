package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class FieldServiceTests : AbstractTest() {

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun testGetQueryFields() {
        // fails in travis, need to find out why.
        fieldService.invalidateFields()
        val fields = fieldService.getQueryFields()

        println(fields)
        assertTrue(fields.containsKey("media.content"))
        assertTrue(fields.containsKey("media.title"))
        assertTrue(fields.containsKey("media.description"))
        assertTrue(fields.containsKey("source.keywords"))
    }
}
