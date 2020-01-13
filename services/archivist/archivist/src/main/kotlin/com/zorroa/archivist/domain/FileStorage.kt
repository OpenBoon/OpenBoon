package com.zorroa.archivist.domain

import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.FileUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * Internal enum class which describes the file storage group
 */
enum class FileGroup {

    /**
     * The stored file is associated with an asset.
     */
    ASSET,

    /**
     * The stored file is for internal use.
     */
    INTERNAL;

    fun lower() = this.toString().toLowerCase()
}

/**
 * Internal class for the category of FileStorage.
 */
enum class FileCategory {

    /**
     * The file is considered a source file.  These are typically added by file uploads."
     */
    SOURCE,

    /**
     * The file is a proxy or alternative low resolution representation."
     */
    PROXY,

    /**
     * The file is some form of metadata, text, json, etc."
     */
    METADATA,

    /**
     * The file is a encryption key.
     */
    KEYS,

    /**
     * The file is elated to an element.
     */
    ELEMENT,

    /**
     * Configuration files.
     */
    CONFIG;

    fun lower() = this.toString().toLowerCase()
}

@ApiModel("FileStorageAttrs", description = "Additional attributes that can be stored with a file.")
class FileStorageAttrs(

    @ApiModelProperty("The name of the file, overrides the local file name.")
    var name: String,

    @ApiModelProperty("The file used internally by ZMLP.  Internal files cannot be created by REST calls.")
    var attrs: Map<String, Any>
)

/**
 * FileStorageLocator Interface defines the base properties and methods
 * a file stored in CloudStorage.
 */
interface CloudStorageLocator {

    /**
     * The category is the overall group of file.
     */
    val category: FileCategory

    /**
     * The actual name of the file.
     */
    val name: String

    /**
     * The full path into bucket storage where the file is stored.
     */
    fun getPath(): String
}

class SystemFileLocator(
    override val category: FileCategory,
    override val name: String
) : CloudStorageLocator  {

    override fun getPath(): String {
        return "system/${category.name.toLowerCase()}/$name"
    }
}

/**
 * Internal class for storing the location details of a project based file.
 *
 * @property type The type of ZMLP object.
 * @property id The id of the object.
 * @property category The category the object belongs in, this is just the directory it lives in.
 * @property name The name of the file.
 * @property projectId An optional projectId for superadmin ops.
 */
class ProjectFileLocator(
    val group: FileGroup,
    val id: String,
    override val category: FileCategory,
    override val name: String,
    val projectId: UUID? = null
) : CloudStorageLocator {

    override fun getPath(): String {

        if (name.lastIndexOf('.') == -1) {
            throw IllegalArgumentException("File name has no extension: $name")
        }

        if ("/" in name) {
            throw IllegalArgumentException("File name cannot contain slashes")
        }

        val proj = projectId ?: getProjectId()
        return "projects/$proj/${group.lower()}/$id/${category.lower()}/$name"
    }
}

/**
 * Internal class for storing a file against a [CloudStorageLocator]
 *
 * @property locator The location of the file.
 * @property attrs Arbitrary attrs to store with the file.
 * @property data The actual file data in the form of a ByteArray
 * @property mimetype The mimetype (aka MediaType) of the file which is auto detected.
 */
class FileStorageSpec(
    val locator: CloudStorageLocator,
    var attrs: Map<String, Any>,
    val data: ByteArray

) {
    val mimetype = FileUtils.getMediaType(locator.getPath())
}

@ApiModel("FileStorage", description = "Describes a file stored in ZMLP storage.")
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
    var attrs: Map<String, Any>,

    @ApiModelProperty("Overrides which Asset")
    var sourceAssetId: String? = null
)
