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
    val search: AssetSearch? = null

)

/**
 * The response sent back when Links are updated.
 *
 * @property updatedAssetIds: The number of links added.  A duplicate link is considered success.
 * @peoperty erroredAssetIds: Assets that were not linked due to some type of error.
 */
class UpdateLinksResponse(val updatedAssetIds: Set<String>, val erroredAssetIds: Set<String>)

class LinkSchema : HashMap<String, MutableSet<UUID>>() {

    fun addLink(type: String, target: UUID): Boolean {
        var set = this[type]
        return if (set == null) {
            set = mutableSetOf(target)
            this[type] = set
            true
        } else {
            set.add(target)
        }
    }

    fun addLink(type: String, target: String): Boolean {
        return addLink(type, UUID.fromString(target))
    }

    fun addLink(type: LinkType, target: UUID): Boolean {
        return addLink(type.key(), target)
    }

    fun removeLink(type: LinkType, target: UUID): Boolean {
        this[type.key()]?.let {
            return it.remove(target)
        }
        return false
    }
}
