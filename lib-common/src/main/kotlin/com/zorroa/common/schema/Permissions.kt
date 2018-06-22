package com.zorroa.common.schema

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*

/**
 * PermissionSchema contains the attributes necessary for defining permissions around
 * assets.
 *
 *
 *
 * There are currently three permission types:
 *
 *  * search - users that can see the asset in a search.
 *  * export - users that can export the source material.
 *  * write - users that can modify the asset.
 *
 */
class PermissionSchema {

    var read: MutableSet<UUID> = mutableSetOf()
    var export: MutableSet<UUID> = mutableSetOf()
    var write: MutableSet<UUID> = mutableSetOf()

    val isEmpty: Boolean
        @JsonIgnore
        get() = export.size + write.size + read.size == 0

    fun setSearch(read: MutableSet<UUID>): PermissionSchema {
        this.read = read
        return this
    }

    fun setExport(export: MutableSet<UUID>): PermissionSchema {
        this.export = export
        return this
    }

    fun setWrite(write: MutableSet<UUID>): PermissionSchema {
        this.write = write
        return this
    }

    fun addToExport(vararg values: UUID) {
        for (i in values) {
            export.add(i)
        }
    }

    fun addToRead(vararg values: UUID) {
        for (i in values) {
            read.add(i)
        }
    }

    fun addToWrite(vararg values: UUID) {
        for (i in values) {
            write.add(i)
        }
    }

    fun removeFromExport(vararg values: UUID) {
        for (i in values) {
            export.remove(i)
        }
    }

    fun removeFromRead(vararg values: UUID) {
        for (i in values) {
            read.remove(i)
        }
    }

    fun removeFromWrite(vararg values: UUID) {
        for (i in values) {
            write.remove(i)
        }
    }
}
