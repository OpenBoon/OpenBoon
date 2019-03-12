package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import org.springframework.security.core.GrantedAuthority
import java.util.*

class SetPermissions constructor() {

    var acl: Acl? = null
    var replace = false

    constructor(acl:Acl?, replace:Boolean) : this() {
        this.acl = acl
        this.replace = replace
    }
}

class PermissionUpdateSpec(
        val id: UUID,
        val type: String,
        val name: String,
        val description: String
)

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

class Permission constructor (
        val id: UUID,
        val name: String,
        val type: String,
        val description: String,
        @JsonIgnore val immutable: Boolean=false) : GrantedAuthority {

    val fullName = "$type$JOIN$name"

    override fun getAuthority(): String {
        return fullName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Permission

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        const val JOIN = "::"
    }
}

class PermissionFilter constructor(
        val types: List<String>? = null,
        val names: List<String>? = null,
        val authorities: List<String>? = null,
        val assignableToUser: Boolean? = null,
        val assignableToObj: Boolean? = null
) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
            mapOf("id" to "permission.pk_permission",
                    "name" to "permission.str_name",
                    "type" to "permission.str_type",
                    "authority" to "permission.str_authority")

    override fun build() {
        if (sort == null) {
            sort = listOf("authority:a")
        }

        addToWhere("permission.pk_organization=?")
        addToValues(getOrgId())

        types?.let  {
            addToWhere(com.zorroa.common.util.JdbcUtils.inClause("permission.str_type", it.size))
            addToValues(it)
        }

        names?.let  {
            addToWhere(com.zorroa.common.util.JdbcUtils.inClause("permission.str_name", it.size))
            addToValues(it)
        }

        authorities?.let  {
            addToWhere(com.zorroa.common.util.JdbcUtils.inClause("permission.str_authority", it.size))
            addToValues(it)
        }

        assignableToUser?.let {
            addToWhere("bool_assignable_to_user=?")
            addToValues(assignableToUser)
        }

        assignableToObj?.let {
            addToWhere("bool_assignable_to_obj=?")
            addToValues(assignableToObj)
        }

    }
}
