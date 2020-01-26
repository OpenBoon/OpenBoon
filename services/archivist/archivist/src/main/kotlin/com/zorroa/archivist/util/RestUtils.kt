package com.zorroa.archivist.util

import com.zorroa.zmlp.service.logging.LogObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Some utilities for returning basic response bodies.
 */
object RestUtils {

    fun updated(type: LogObject, id: Any): Map<String, Any> {
        return mapOf("type" to type.toString(), "id" to id, "op" to "update")
    }
}

/**
 * A subclass of ByteArrayOutputStream that provides direct access
 * to the underlying byte array.
 */
class RawByteArrayOutputStream(size: Int) : ByteArrayOutputStream(size) {

    fun toInputStream(): InputStream {
        return ByteArrayInputStream(buf, 0, count)
    }
}
