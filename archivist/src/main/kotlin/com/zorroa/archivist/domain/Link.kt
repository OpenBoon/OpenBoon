package com.zorroa.archivist.domain

import java.util.*

enum class LinkType {
    Folder,
    Import,
    Export;

    fun key() : String {
        return this.toString().toLowerCase()
    }
}

class ModifyLinksResponse {
        val success = mutableSetOf<String>()
        val failed = mutableSetOf<String>()
}


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
