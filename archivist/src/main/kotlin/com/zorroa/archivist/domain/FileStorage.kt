package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.ServableFile
import java.net.URI
import java.util.*

/**
 * The attributes needed to register storage.
 *
 * @property parentType The parent type the file is associated with.
 * @property parentId The parent ID the file is associated with.
 * @property name A name for the file.
 */
class FileStorageSpec(
        val parentType: String,
        val parentId: String,
        val name: String

) {

    constructor(parentType: String, parentId: UUID, name: String) : this(parentType, parentId.toString(), name)

    override fun toString(): String {
        return "FileStorageSpec(parentType='$parentType', parentId='$parentId', name='$name')"
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
        val uri: URI,
        val scheme: String,
        val mediaType: String,
        @JsonIgnore val fileServerProvider: FileServerProvider
) {

    constructor(id: String, uri: String, scheme: String, mediaType: String, fileServerProvider: FileServerProvider) :
            this(id, URI(uri), scheme, mediaType, fileServerProvider)

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


