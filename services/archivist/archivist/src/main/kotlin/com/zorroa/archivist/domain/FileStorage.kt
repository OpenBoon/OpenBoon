package com.zorroa.archivist.domain

import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.FileUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * Internal class for the category of FileStorage.
 */
@ApiModel("FileCategory", description = "The category of the stored file")
enum class FileCategory {

    @ApiModelProperty("The file is considered a source file.  These are typically added by file uploads.")
    SOURCE,

    @ApiModelProperty("The file is a proxy or alternative low resolution representation.")
    PROXY,

    @ApiModelProperty("The file is some form of metadata, text, json, etc.")
    METADATA,

    @ApiModelProperty("The file used internally by PixelML.  Internal files cannot be created by REST calls.")
    INTERNAL;

    fun lower() = this.toString().toLowerCase()
}

@ApiModel("FileStorageAttrs", description = "Additional attributes that can be stored with a file.")
class FileStorageAttrs(

    @ApiModelProperty("The file used internally by PixelML.  Internal files cannot be created by REST calls.")
    var attrs: Map<String, Any>
)

/**
 * Internal class for storing the location details of a file.
 *
 * @property type The type of PixelML object.
 * @property id The id of the object.
 * @property category The category the object belongs in, this is just the directory it lives in.
 * @property name The name of the file.
 * @property projectId An optional projectId for superadmin ops.
 */
class FileStorageLocator(

    val type: LogObject,
    val id: String,
    val category: FileCategory,
    val name: String,
    val projectId: UUID? = null
) {

    fun getPath() : String {

        if (name.lastIndexOf('.') == -1) {
            throw IllegalArgumentException("File name has no extension: $name")
        }

        if ("/" in name) {
            throw IllegalArgumentException("File name cannot contain slashes")
        }

        val proj = projectId ?: getProjectId()
        return "projects/$proj/${type.lower()}/$id/${category.lower()}/$name"
    }
}

/**
 * Internal class for storing a file against a [FileStorageLocator]
 *
 * @property locator The location of the file.
 * @property attrs Arbitrary attrs to store with the file.
 * @property data The actual file data in the form of a ByteArray
 * @property mimetype The mimetype (aka MediaType) of the file which is auto detected.
 */
class FileStorageSpec  (
    val locator: FileStorageLocator,
    var attrs: Map<String, Any>,
    val data: ByteArray

) {
    val mimetype = FileUtils.getMediaType(locator.name)
}


@ApiModel("FileStorage", description = "Describes a file stored in PixelML storage.")
class FileStorage(

    @ApiModelProperty("The file name")
    val name: String,

    @ApiModelProperty("The category of file.")
    val category: String,

    @ApiModelProperty("The mimetype of the file, detected from the extension.")
    val mimetype: String,

    @ApiModelProperty("The size of the file.")
    val size: Long,

    @ApiModelProperty("Arbitrary attributes for the file")
    var attrs: Map<String, Any>
)
