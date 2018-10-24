package com.zorroa.archivist.domain

import java.util.*

/**
 * The attributes needed to register storage.
 *
 * @property category The name of the file category
 * @property name A globally unique name for the file.
 * @property type The file type
 * @property variants Additional naming variants
 */
class FileStorageSpec(
        val category: String,
        val name: String,
        val type: String,
        val variants: List<String>?=null,
        var assetId: UUID?=null,
        var jobId: UUID?=null,
        var taskId: UUID?=null

) {
    override fun toString(): String {
        return "FileStorageSpec(category='$category', name='$name', type='$type', variants=$variants)"
    }
}

/**
 * The result of registering file storage.

 * @property uri The full URI to the file.
 * @property id The ID for the file, is used for streaming the file from the Archivist.
 * @property scheme The URI scheme, will be file, http(s).
 * @property mimeType A mime type based off the file extension.
 * @property size: The size of the file.
 * @property exists: If the file exists or not.
 */
class FileStorage(
        val id: String,
        val uri: String,
        val scheme: String,
        val mimeType: String,
        val size: Long,
        val exists: Boolean


) {
    override fun toString(): String {
        return "FileStorage(uri='$uri', id='$id', scheme='$scheme', mimeType='$mimeType')"
    }
}


