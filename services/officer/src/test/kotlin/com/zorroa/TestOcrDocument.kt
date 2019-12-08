package com.zorroa

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Test
import java.io.FileInputStream
import kotlin.test.assertTrue

class TestOcrDocument {

    @Test
    fun testRenderAllMetadata() {
        val options = Options("src/test/resources/ocr_test1.tif")
        val doc = OcrDocument(options, FileInputStream(options.fileName))
        doc.render()

        val metadata = Json.mapper.readValue(doc.getMetadata(), Map::class.java)
        assertTrue(metadata.containsKey("content"))
    }

    @Test
    fun testRenderMetadata() {
        val options = Options("src/test/resources/ocr_test1.tif")
        options.page = 1
        val doc = OcrDocument(options, FileInputStream(options.fileName))
        doc.render()

        val metadata = Json.mapper.readValue(doc.getMetadata(), Map::class.java)
        assertTrue(metadata.containsKey("content"))
    }
}