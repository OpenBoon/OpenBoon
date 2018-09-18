package com.zorroa.archivist.domain

import java.util.*

class FileStorageRequestSpec(
        val name: String,
        val group: String,
        // Optional and can be detected by server but will be slower
        var mimeType: String?,
        var size: Long?
)

/**
 * @param name: name of the file
 * @param group: The group the file is in, "proxy", "whatever".
 * @param url: the URL where the file will go, will be file:// or url://
 */
class FileStorageRequest(
        val name: String,
        val group: String,
        val url: String)


