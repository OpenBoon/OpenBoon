package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.MoreObjects
import com.zorroa.archivist.repository.DaoFilter
import com.zorroa.archivist.search.AssetSearch
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.Objects
import java.util.UUID

class FolderFilter : DaoFilter() {

    var paths: List<String>? = null

    override val sortMap: Map<String, String>?
        get() = mapOf("name" to "str_name")

    override fun build() {
    }
}

fun isRootFolder(folder: Folder): Boolean {
    return folder.name == "/" && folder.parentId == null
}

@ApiModel("Folder", description = "Describes a Folder.")
data class Folder(

    @ApiModelProperty("UUID of the Folder.")
    val id: UUID,

    @ApiModelProperty("Name of the Folder.")
    val name: String,

    @ApiModelProperty("UUID of the this Folder's parent Folder.")
    val parentId: UUID?,

    @ApiModelProperty("UUID of the Organization this Folder belongs to.")
    val organizationId: UUID,

    @ApiModelProperty("UUID of the Dynamic Hierarchy this Folder belongs to.")
    val dyhiId: UUID?,

    @ApiModelProperty("User this Folder belongs to.")
    val user: UserBase,

    @ApiModelProperty("Time this Folder was creates.")
    val timeCreated: Long,

    @ApiModelProperty("Time this Folder was last modified.")
    val timeModified: Long,

    @ApiModelProperty(hidden = true)
    val recursive: Boolean,

    @ApiModelProperty("If true this Folder is the root of a Dynamic Hierarchy.")
    val dyhiRoot: Boolean,

    @ApiModelProperty("Field this Folder is based on in a Dynamic Hierarchy.")
    val dyhiField: String?,

    @ApiModelProperty("Number of children this Folder has.")
    val childCount: Int = 0,

    @ApiModelProperty("ACL applied to this Folder.")
    val acl: Acl? = null,

    @ApiModelProperty("Search filter responsible for populating this Folder.")
    val search: AssetSearch? = null,

    @ApiModelProperty("If true this Folder is the root of a taxonomy.")
    var taxonomyRoot: Boolean = false,

    @ApiModelProperty("Attributes for this Folder.")
    val attrs: Map<String, Any>? = null

) {

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("parentId", parentId)
                .add("name", name)
                .toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as Folder
        return id == that.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}

@ApiModel("Folder Update", description = "Describes an update made to a Folder.")
data class FolderUpdate(

    @ApiModelProperty("Name of the Folder.")
    var name: String,

    @ApiModelProperty("UUID of the Folder's parent Folder.")
    var parentId: UUID?,

    @ApiModelProperty("If true the Folder is recursive.")
    var recursive: Boolean = true,

    @ApiModelProperty("Search filter used to populate the Folder.")
    var search: AssetSearch? = null,

    @ApiModelProperty("Folder's attributes.")
    var attrs: Map<String, Any>? = null

) {

    constructor(folder: Folder) :
            this(folder.name, folder.parentId, folder.recursive, folder.search, folder.attrs)
}

@ApiModel("Folder Spec", description = "Attributes required to create a Folder.")
class FolderSpec {

    @ApiModelProperty("Name of the Folder")
    var name: String? = null

    @ApiModelProperty("UUID of the Folder's parent Folder.")
    var parentId: UUID? = null

    @ApiModelProperty("UUID of a Dynamic Hierarchy to apply to this folder.")
    var dyhiId: UUID? = null

    @ApiModelProperty("Search filter to use to populate this Folder.")
    var search: AssetSearch? = null

    @ApiModelProperty("ACL applied to the Folder.")
    var acl: Acl? = null

    @ApiModelProperty("Folder attributes.")
    var attrs: Map<String, Any>? = null

    @ApiModelProperty("If true the Folder will be recursive.")
    var recursive: Boolean = true

    @JsonIgnore
    var created: Boolean = false

    @JsonIgnore
    var userId: UUID? = null

    constructor()

    constructor(name: String) :
            this(name, null)

    constructor(name: String, folder: Folder) :
            this(name, folder.id)

    constructor(name: String, parentId: UUID?) {
        this.name = name
        this.parentId = parentId
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("parentId", parentId)
                .add("name", name)
                .toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as FolderSpec
        return parentId == that.parentId && name == that.name
    }

    override fun hashCode(): Int {
        return Objects.hash(parentId, name)
    }
}
