package com.zorroa.archivist.domain

import com.zorroa.archivist.search.AssetSearch
import java.util.*

enum class LinkType {
    Folder,
    Import,
    Export;

    fun key() : String {
        return this.toString().toLowerCase()
    }
}

/**
 * BatchUpdateAssetLinks defines an arbitrarily large set of assets which need
 * to be linked.
 *
 * @property: assetIds - the asset IDs to be linked.
 * @property: parentIds - the parents IDs to be combined with the search.
 * @property: search - a search which when combined with parentIds wil yield children
 */
class BatchUpdateAssetLinks(
        val assetIds:  List<String>? = null,
        val parentIds : List<String>? = null,
        val search: AssetSearch?=null
)

/**
 * The response sent back when Links are updated.
 *
 * @property updatedAssetIds: The number of links added.  A duplicate link is considered success.
 * @peoperty erroredAssetIds: Assets that were not linked due to some type of error.
 */
class UpdateLinksResponse(val updatedAssetIds : Set<String>, val erroredAssetIds : Set<String>)

class LinkSchema : HashMap<String, MutableSet<UUID>>() {

    fun addLink(type: String, target: UUID) : Boolean {
        var set = this[type]
        return if (set == null) {
            set = mutableSetOf(target)
            this[type] = set
            true
        }
        else {
            set.add(target)
        }
    }

    fun addLink(type: String, target: String) : Boolean {
        return addLink(type, UUID.fromString(target))
    }

    fun addLink(type: LinkType, target: UUID) : Boolean {
        return addLink(type.key(), target)
    }

    fun removeLink(type: LinkType, target: UUID) : Boolean {
        this[type.key()]?.let {
            return it.remove(target)
        }
        return false
    }
}
