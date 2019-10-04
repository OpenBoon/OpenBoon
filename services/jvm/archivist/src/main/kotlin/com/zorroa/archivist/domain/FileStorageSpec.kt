package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.ServableFile
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.net.URI
import java.util.UUID

@ApiModel("File Storage Spec", description = "Attributes needed to register file with the storage backend.")
class FileStorageSpec(

    @ApiModelProperty("Parent type the file is associated with.")
    val parentType: String,

    @ApiModelProperty("Parent ID the file is associated with.")
    val parentId: String,

    @ApiModelProperty("Name of the file.")
    val name: String

) {

    constructor(parentType: String, parentId: UUID, name: String) : this(parentType, parentId.toString(), name)

    override fun toString(): String {
        return "FileStorageSpec(parentType='$parentType', parentId='$parentId', name='$name')"
    }
}

@ApiModel("File Storage", description = "Describes a file stored using the File Storage backend.")
class FileStorage(

    @ApiModelProperty("UUID of the File Storage object.")
    val id: String,

    @ApiModelProperty("URI to the location of the file.")
    val uri: URI,

    @ApiModelProperty("URI scheme for the file's location.", allowableValues = "file,http,https")
    val scheme: String,

    @ApiModelProperty("Mimetype of the file.")
    val mediaType: String,

    @JsonIgnore
    val fileServerProvider: FileServerProvider

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
     * @return ServableFile
     */
    fun getServableFile(): ServableFile {
        return fileServerProvider.getServableFile(uri)
    }
}
