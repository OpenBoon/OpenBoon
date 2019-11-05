package com.zorroa

import org.junit.Test
import kotlin.test.assertTrue

class TestOcrDocument {

    @Test
    fun testRenderAllMetadata() {
        val options = Options("src/test/resources/ocr_test1.tif")
        val doc = OcrDocument(options)
        doc.render()

        val metadata = Json.mapper.readValue(doc.getMetadataFile(1), Map::class.java)
        assertTrue(metadata.containsKey("content"))
    }

    @Test
    fun testRenderMetadata() {
        val options = Options("src/test/resources/ocr_test1.tif")
        options.page = 1
        val doc = OcrDocument(options)
        doc.render()

        val metadata = Json.mapper.readValue(doc.getMetadataFile(1), Map::class.java)
        assertTrue(metadata.containsKey("content"))
    }
}