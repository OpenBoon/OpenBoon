package com.zorroa

import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// There is more but they could be null and stripped out.
val requiredAssetFields = setOf("type", "timeCreated", "width", "height", "orientation")

fun validateAssetMetadata(stream: InputStream): Map<String, Any> {
    val metadata = Json.mapper.readValue<Map<String, Any>>(stream)
    assertFalse(metadata.containsKey("content"))
    assertEquals("document", metadata["type"])
    requiredAssetFields.forEach {
        assertTrue(metadata.containsKey(it), "Asset metadata did not contain '$it'")
    }
    return metadata
}

val requiredPageFields = setOf("width", "height", "orientation", "content")

fun validatePageMetadata(stream: InputStream) {
    val metadata = Json.mapper.readValue(stream, Map::class.java)
    assertNull(metadata["type"])
    requiredPageFields.forEach {
        assertTrue(metadata.containsKey(it), "Page metadata did not contain '$it'")
    }
}
