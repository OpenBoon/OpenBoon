package com.zorroa.zmlp.apikey

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * The minimal properties for a ZMLP Actor.
 */
@ApiModel("ZmlpActor", description = "An authenticated ApiKey")
class ZmlpActor(

    @ApiModelProperty("The unique ID of the ApiKey")
    val id: UUID,

    @ApiModelProperty("The project ID of the ApiKey")
    val projectId: UUID,

    @ApiModelProperty("A unique name or label.")
    val name: String,

    @ApiModelProperty("A list of permissions associated with key.")
    val permissions: Set<Permission>,

    var attrs: MutableMap<String, String> = mutableMapOf()
) {
    fun hasAnyPermission(vararg perm: Permission): Boolean {
        return perm.any { it in permissions }
    }

    override fun toString(): String {
        return "$id/$name"
    }

    fun getAttr(key: String): String? {
        return attrs[key]
    }

    fun setAttr(key: String, value: String) {
        attrs[key] = value
    }
}
