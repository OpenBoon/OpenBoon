package com.zorroa.common.schema

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.UUID

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
 *  * delete - users that can delete the asset.
 *
 */
class PermissionSchema {

    var read: MutableSet<UUID> = mutableSetOf()
    var export: MutableSet<UUID> = mutableSetOf()
    var write: MutableSet<UUID> = mutableSetOf()
    var delete: MutableSet<UUID> = mutableSetOf()

    val isEmpty: Boolean
        @JsonIgnore
        get() = export.size + write.size + read.size + delete.size == 0

    fun copy(): PermissionSchema {
        val result = PermissionSchema()
        result.read = read.toMutableSet()
        result.write = write.toMutableSet()
        result.export = export.toMutableSet()
        result.delete = delete.toMutableSet()
        return result
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

    fun addToDelete(vararg values: UUID) {
        for (i in values) {
            delete.add(i)
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

    fun removeFromDelete(vararg values: UUID) {
        for (i in values) {
            delete.remove(i)
        }
    }
}
