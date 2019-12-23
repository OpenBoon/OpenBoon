package com.zorroa

import java.io.InputStream
import kotlin.test.assertTrue

val requiredPageFields = setOf(
    "width", "height", "orientation", "content",
    "type", "timeCreated", "width", "height", "orientation"
)

fun validateMetadata(stream: InputStream, vararg skip: String) {
    val metadata = Json.mapper.readValue(stream, Map::class.java)
    requiredPageFields.forEach {
        if (it !in skip) {
            assertTrue(metadata.containsKey(it), "Page metadata did not contain '$it'")
        }
    }
}
