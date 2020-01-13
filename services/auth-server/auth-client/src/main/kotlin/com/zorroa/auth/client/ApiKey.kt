package com.zorroa.auth.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.Base64
import java.util.Calendar
import java.util.Date
import java.util.UUID

/**
 * The minimum properties needed for a valid API signing key.
 */
@ApiModel("SigningiKey", description = "The attributes required to sign JWT requests.")
class SigningiKey(

    @ApiModelProperty("Uniquely identifies account")
    val accessKey: String,

    @ApiModelProperty("A shared key used to sign API requests.")
    val secretKey: String

    ) {
    fun toBase64(): String {
        return Base64.getEncoder().encodeToString(Json.mapper.writeValueAsBytes(this))
    }
}

@ApiModel("ApiKey", description = "An API key allows remote users to access ZMLP resources.")
class ApiKey(

    @ApiModelProperty("The unique ID of the ApiKey")
    val id: UUID,

    @ApiModelProperty("The Project ID the ApiKey belongs in.")
    val projectId: UUID,

    @ApiModelProperty("Uniquely identifies account")
    val accessKey: String,

    @ApiModelProperty("A shared key used to sign API requests.")
    val secretKey: String,

    @ApiModelProperty("A unique name for the key.")
    val name: String,

    @ApiModelProperty("The permissions or roles for the ApiKey")
    val permissions: Set<Permission>
) {

    @JsonIgnore
    fun getJwtToken(timeout: Int = 60, projId: UUID? = null): String {
        val algo = Algorithm.HMAC512(secretKey)
        val spec = JWT.create().withIssuer("zmlp")
            .withClaim("accessKey", accessKey)

        if (timeout > 0) {
            val c = Calendar.getInstance()
            c.time = Date()
            c.add(Calendar.SECOND, timeout)
            spec.withExpiresAt(c.time)
        }
        return spec.sign(algo)
    }

    @JsonIgnore
    fun getSigningKey(): SigningiKey {
        return SigningiKey(accessKey, secretKey)
    }

    @JsonIgnore
    fun getZmlpActor(): ZmlpActor {
        return ZmlpActor(id, projectId, name, permissions)
    }

    fun toBase64(): String {
        return Base64.getEncoder().encodeToString(
            Json.mapper.writeValueAsBytes(getSigningKey())
        )
    }

    override fun toString(): String {
        return "ApiKey(id=$id, projectId=$projectId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApiKey

        if (id != other.id) return false
        if (projectId != other.projectId) return false
        if (secretKey != other.secretKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + projectId.hashCode()
        result = 31 * result + secretKey.hashCode()
        return result
    }
}

fun UUID.prefix(size: Int = 8): String {
    return this.toString().substring(0, size)
}
