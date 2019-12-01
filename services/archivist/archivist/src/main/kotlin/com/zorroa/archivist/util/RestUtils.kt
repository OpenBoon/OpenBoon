package com.zorroa.archivist.util

import com.zorroa.archivist.domain.LogObject

/**
 * Some utilities for returning basic response bodies.
 */
object RestUtils {

    fun updated(type: LogObject, id: Any): Map<String, Any> {
        return mapOf("type" to type.toString(), "id" to id, "op" to "update")
    }
}