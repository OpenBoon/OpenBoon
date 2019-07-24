package com.zorroa.archivist.domain

import com.zorroa.archivist.search.AssetSearch
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

enum class LinkType {
    Folder,
    Import,
    Export;

    fun key(): String {
        return this.toString().toLowerCase()
    }
}

@ApiModel("Batch Update Asset Links", description = "Defines an arbitrarily large set of assets which need to be linked.")
class BatchUpdateAssetLinks(

    @ApiModelProperty("UUIDs of Assets to be linked.")
    val assetIds: List<String>? = null,

    @ApiModelProperty("UUIDs of parents to be combined with the search.")
    val parentIds: List<String>? = null,

    @ApiModelProperty("Search which when combined with parentIds wil yield children")
    val search: AssetSearch? = null,

    @ApiModelProperty("Replace links rather than append.  Default is to append")
    val replace : Boolean = false
)

/**
 * The response sent back when Links are updated.
 *
 * @property updatedAssetIds: The number of links added.  A duplicate link is considered success.
 * @property erroredAssetIds: Assets that were not linked due to some type of error.
 */
class UpdateLinksResponse(val updatedAssetIds: Set<String>, val erroredAssetIds: Set<String>)

/**
 * A wrapper class for manipulating Links.  Links do things like determine what
 * folder an asset lives in, what exports it was part of, etc.
 */
class LinkSchema : HashMap<String, MutableSet<UUID>>() {

    /**
     * Reset the links for a given type to the given list of UUIDs.
     *
     * @param type The link type.
     * @param target The list of target IDs.
     */
    fun setLinks(type: LinkType, target: List<UUID>) {
        var key = type.key()
        var set = this[key]
        if (set == null) {
            set = target.toMutableSet()
            this[key] = set
        } else {
            set.addAll(target)
        }
    }

    /**
     * Add the links for a given type to any existing links of the same type.
     *
     * @param type The link type.
     * @param target The list of target IDs.
     */
    fun addLinks(type: LinkType, target: List<UUID>): Boolean {
        val key = type.key()
        var set = this[key]
        return if (set == null) {
            set = target.toMutableSet()
            this[key] = set
            true
        } else {
            set.addAll(target)
        }
    }

    /**
     * Remove the given links of the specified type.
     *
     * @param type The link type.
     * @param target The list of target IDs.
     */
    fun removeLinks(type: LinkType, target: List<UUID>): Boolean {
        this[type.key()]?.let {
            return it.removeAll(target)
        }
        return false
    }
}
