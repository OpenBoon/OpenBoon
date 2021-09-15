package boonai.archivist.util

import boonai.common.service.logging.LogObject
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.servlet.http.HttpServletRequest

/**
 * Some utilities for returning basic response bodies.
 */
object RestUtils {

    fun updated(type: LogObject, id: Any): Map<String, Any> {
        return mapOf("type" to type.toString().lowercase(), "id" to id, "op" to "update")
    }

    /**
     * A standard batch update response.
     */
    fun batchUpdated(type: LogObject, op: String, updated: Int, errors: Int): MutableMap<String, Any> {
        return mutableMapOf(
            "type" to type.toString().lowercase(),
            "op" to op,
            "updated" to updated,
            "errors" to errors
        )
    }

    fun batchSubmitted(type: LogObject, op: String, updated: Int, errors: Int): MutableMap<String, Any> {
        return mutableMapOf(
            "type" to type.toString().lowercase(),
            "op" to op,
            "submitted" to updated,
            "errors" to errors
        )
    }

    fun status(type: String, id: Any, op: String, success: Boolean): Map<String, Any> {
        return mapOf("type" to type, "id" to id, "op" to op, "success" to success)
    }

    fun status(type: String, op: String, success: Boolean): Map<String, Any> {
        return mapOf("type" to type, "op" to op, "success" to success)
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

/**
 * Return the current http request or null if one does not exist.
 */
fun getCurrentHttpRequest(): HttpServletRequest? {
    return (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
}
