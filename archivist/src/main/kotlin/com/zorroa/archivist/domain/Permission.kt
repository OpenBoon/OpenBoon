package com.zorroa.archivist.domain

class SetPermissions constructor() {

    var acl: Acl? = null
    var replace = false

    constructor(acl:Acl?, replace:Boolean) : this() {
        this.acl = acl
        this.replace = replace
    }
}

class PermissionSpec {

    var name: String? = null
    var type: String? = null
    var source: String = "local"
    var description: String? = null

    constructor()

    constructor(authority: String) {
        val parts = authority.split(Permission.JOIN, limit=2).toTypedArray()
        if (parts.size == 1) {
            throw IllegalArgumentException("Invalid authority name: $authority")
        }
        this.name = parts[1]
        this.type = parts[0]
    }

    constructor(type: String, name: String) {
        this.name = name
        this.type = type
    }

}

