package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.FileUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * Returns the proper [ProjectStorageLocator] implementation for the given settings.
 */
fun getFileLocator(entity: String, id: String, category: String, name: String): ProjectStorageLocator {

    return when (val entityType = ProjectStorageEntity.valueOf(entity.toUpperCase())) {
        ProjectStorageEntity.ASSETS -> {
            AssetFileLocator(id, category, name)
        }
        ProjectStorageEntity.MODELS -> {
            ProjectFileLocator(entityType, category, name)
        }
    }
}

/**
 * Internal enum class which describes the file storage entity,
 * such as an Assets, Models, Jobs, tasks.  The name matches
 * the name used in the REST API.
 */
enum class ProjectStorageEntity {

    /**
     * The stored file is associated with an asset.
     */
    ASSETS,

    /**
     * The stored file is a model.
     */
    MODELS;

    fun lower() = this.toString().toLowerCase()
}

/**
 * Internal class for commonly use storage categories.   Processors can pass up
 * whatever they want but these are used by the backend.
 */
object ProjectStorageCategory {

    /**
     * The file is considered a source file.  These are typically added by file uploads."
     */
    const val SOURCE = "source"

    /**
     * The file is a proxy or alternative low resolution representation."
     */
    const val PROXY = "proxy"
}

@ApiModel("Project Storage Request", description = "Properties needed to store a file into ProjectStorage.")
class ProjectStorageRequest(

    @ApiModelProperty("The name of the file, overrides the local file name.")
    var name: String,

    @ApiModelProperty("The category of the file.")
    var category: String,

    @ApiModelProperty("The file used internally by ZMLP.  Internal files cannot be created by REST calls.")
    var attrs: Map<String, Any>,

    @ApiModelProperty("The entity the file is related to.")
    var entity: ProjectStorageEntity? = null
)

/**
 * The ProjectStorageLocator Interface defines the based properties needed
 * for a project storage locator. A Locator handles converting properties
 * into an actual bucket path.
 */
interface ProjectStorageLocator {

    /**
     * The category is the final directory before the file.
     */
    val category: String

    /**
     * The actual name of the file.
     */
    val name: String

    /**
     * The full path into bucket storage where the file is stored.
     */
    fun getPath(): String

    /**
     * A partial URI path that can be used with the Archivist URI.
     */
    fun getFileId(): String
}

/**
 *
 */
class ProjectFileLocator(
    val entity: ProjectStorageEntity,
    override val category: String,
    override val name: String,
    val id: String? = null,
    @JsonIgnore
    val projectId: UUID? = null
) : ProjectStorageLocator {

    override fun getPath(): String {
        val pid = projectId ?: getProjectId()
        return if (id != null) {
            "projects/$pid/${entity.lower()}/$category/$name"
        } else {
            "projects/$pid/${entity.lower()}/$id/$category/$name"
        }
    }

    override fun getFileId(): String {
        return "${entity.lower()}/$id/$category/$name"
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
class AssetFileLocator(
    val id: String,
    override val category: String,
    override val name: String,
    @JsonIgnore
    val projectId: UUID? = null
) : ProjectStorageLocator {

    val entity = ProjectStorageEntity.ASSETS

    override fun getFileId(): String {
        return "${entity.lower()}/$id/$category/$name"
    }

    override fun getPath(): String {

        if (name.lastIndexOf('.') == -1) {
            throw IllegalArgumentException("File name has no extension: $name")
        }

        if ("/" in name || ".." in name) {
            throw IllegalArgumentException("Illegal characters in file name")
        }

        val proj = projectId ?: getProjectId()
        return "projects/$proj/$id/${entity.lower()}/$category/$name"
    }
}

/**
 * Internal class for storing a file against a [ProjectStorageLocator]
 *
 * @property locator The location of the file.
 * @property attrs Arbitrary attrs to store with the file.
 * @property data The actual file data in the form of a ByteArray
 * @property mimetype The mimetype (aka MediaType) of the file which is auto detected.
 */
class ProjectStorageSpec(
    val locator: ProjectStorageLocator,
    var attrs: Map<String, Any>,
    val data: ByteArray

) {
    val mimetype = FileUtils.getMediaType(locator.getPath())
}

@ApiModel("FileStorage", description = "Describes a file stored in ZMLP storage.")
class FileStorage(

    @ApiModelProperty("The file name")
    val id: String,

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
) {
    companion object {
        val JSON_LIST_OF: TypeReference<List<FileStorage>> = object : TypeReference<List<FileStorage>>() {}
    }
}
