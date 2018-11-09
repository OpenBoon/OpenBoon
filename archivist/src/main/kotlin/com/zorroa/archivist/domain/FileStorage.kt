package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.ServableFile
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
        var taskId: UUID?=null,
        var create: Boolean=true

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
 * @property mediaType A mime type based off the file extension.
 */
class FileStorage(
        val id: String,
        val uri: String,
        val scheme: String,
        val mediaType: String,
        @JsonIgnore val fileServerProvider: FileServerProvider
) {
    override fun toString(): String {
        return "FileStorage(uri='$uri', id='$id', scheme='$scheme', mimeType='$mediaType')"
    }

    @JsonIgnore
    /**
     * Return a ServableFile instance for this storage.
     *
     * @return  ServableFile
     */
    fun getServableFile() : ServableFile {
        return fileServerProvider.getServableFile(uri)
    }
}


