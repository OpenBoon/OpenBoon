package boonai.common.apikey

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonIgnore
import boonai.common.util.Json
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.Base64
import java.util.Calendar
import java.util.Date
import java.util.UUID

/**
 * The minimum properties needed for a valid API signing key.
 *
 * @property accessKey The unique key identifier.
 * @property secretKey A secret key used to sign API requests.
 */
class SigningKey(
    val accessKey: String,
    val secretKey: String

) {
    /**
     * Return a base64 encoded version of this key.
     */
    fun toBase64(): String {
        return Base64.getEncoder().encodeToString(Json.Mapper.writeValueAsBytes(this))
    }

    /**
     * Obtain a signed JWT token for this signing Key.
     *
     * @param timeout The token timeout, defaults to 60 seconds.
     * @param projectId An optional projectId, if this key has access to ore than 1 project.
     *
     * @return A base64 encoded JWT token.
     */
    @JsonIgnore
    fun getJwtToken(timeout: Int = 60, projectId: UUID? = null): String {
        val algo = Algorithm.HMAC512(secretKey)
        val spec = JWT.create().withIssuer("zmlp")
            .withClaim("accessKey", accessKey)

        projectId?.let {
            spec.withClaim("projectId", projectId.toString())
        }

        if (timeout > 0) {
            val c = Calendar.getInstance()
            c.time = Date()
            c.add(Calendar.SECOND, timeout)
            spec.withExpiresAt(c.time)
        }
        return spec.sign(algo)
    }
}

/**
 * A API key record.
 *
 * @param id The unique id the key.
 * @param projectId The projectId the key belongs to.
 * @param accessKey The key's username.
 * @param secretKey The key's password, this field is not populated, you must request a signing key.
 * @param name The name of the key.
 * @param permissions The key's permissions.
 *
 */
@ApiModel("ApiKey", description = "An API key record.")
class ApiKey(

    @ApiModelProperty("The unique ID of the ApiKey")
    val id: UUID,
    @ApiModelProperty("The project ID of the ApiKey")
    val projectId: UUID,
    @ApiModelProperty("The key's username")
    val accessKey: String,
    @ApiModelProperty("The name of the key.")
    val name: String,
    @ApiModelProperty("The key's permissions.")
    val permissions: Set<Permission>,
    @ApiModelProperty("Indicates that is a System Key")
    val systemKey: Boolean = false
) {

    override fun toString(): String {
        return "ApiKey(id=$id, projectId=$projectId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApiKey) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
