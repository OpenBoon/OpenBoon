package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertTrue

class FieldServiceTests : AbstractTest() {

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Ignore("TODO: Fix this test. It fails on Travis but runs locally.")
    @Test
    fun testGetQueryFields() {
        val fields = fieldService.getQueryFields()
        assertTrue(fields.containsKey("media.content"))
        assertTrue(fields.containsKey("media.title"))
        assertTrue(fields.containsKey("media.description"))
        assertTrue(fields.containsKey("source.keywords"))
    }
}
