package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.MoreObjects
import com.zorroa.archivist.repository.DaoFilter
import com.zorroa.sdk.search.AssetSearch
import java.util.*

class FolderFilter : DaoFilter() {

    var paths : List<String>? = null

    override val sortMap: Map<String, String>?
        get() = mapOf("name" to "str_name")

    override fun build() {

    }
}

val ROOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000")

fun getRootFolderId() : UUID {
    return ROOT_ID
}

fun isRootFolder(folder: Folder) : Boolean {
    return folder.id == ROOT_ID
}

fun isRootFolder(id:UUID?) : Boolean {
    if (id == null) {
        return false
    }
    return id == ROOT_ID
}

data class Folder(
     val id: UUID,
     val name: String,
     val parentId: UUID?,
     val organizationId: UUID,
     val dyhiId: UUID?,
     val user: UserBase,
     val timeCreated: Long,
     val timeModified: Long,
     val recursive: Boolean,
     val dyhiRoot: Boolean,
     val dyhiField: String?,
     val childCount: Int = 0,
     val acl: Acl? = null,
     val search: AssetSearch? = null,
     var taxonomyRoot: Boolean = false,
     val attrs: Map<String, Any>? = null) {

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

data class FolderUpdate (
        var name : String,
        var parentId : UUID?,
        var recursive : Boolean = true,
        var search: AssetSearch? = null,
        var attrs: Map<String, Any>? = null) {

    constructor(folder: Folder) :
            this(folder.name, folder.parentId, folder.recursive, folder.search, folder.attrs)
}

class FolderSpec {

    var name : String? = null
    var parentId : UUID? = null
    var dyhiId: UUID? = null
    var search: AssetSearch? = null
    var acl: Acl? = null
    var attrs: Map<String, Any>? = null
    var recursive : Boolean = true

    @JsonIgnore var created : Boolean = false
    @JsonIgnore var userId: UUID? = null

    constructor()

    constructor(name: String) :
            this(name, ROOT_ID)

    constructor(name: String, folder: Folder) :
            this(name, folder.id)

    constructor(name: String, parentId: UUID) {
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



