package com.zorroa.archivist.schema

import com.zorroa.archivist.domain.Document
import org.junit.Test
import kotlin.test.assertTrue


class DocumentTests {

    @Test
    fun testIsEmpty() {
        val doc = Document()
        doc.setAttr("media", mutableMapOf<String, Any>())
        assertTrue(doc.isEmpty("media"))

    }


}