package com.zorroa.archivist.domain

class SetPermissions {
    var acl: Acl? = null
    var replace = false
}

class PermissionSpec {

    var name: String? = null
    var type: String? = null
    var description: String? = null

    constructor()

    constructor(authority: String) {
        val parts = authority.split(Permission.JOIN.toRegex(), 2).toTypedArray()
        if (parts.size == 1) {
            throw IllegalArgumentException("Invalid authority name: " + authority)
        }
        this.name = parts[1]
        this.type = parts[0]
    }

    constructor(type: String, name: String) {
        this.name = name
        this.type = type
    }
}

