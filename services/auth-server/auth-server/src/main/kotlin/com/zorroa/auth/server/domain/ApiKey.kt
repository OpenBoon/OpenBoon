package com.zorroa.auth.server.domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.auth.client.Permission
import com.zorroa.auth.client.ZmlpActor
import com.zorroa.auth.server.repository.StringSetConverter
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

@ApiModel("ApiKey Spec", description = "The attributes required to create a new API key.")
class ApiKeySpec(
    @ApiModelProperty("A unique name or label for the key.")
    val name: String,

    @ApiModelProperty("A list of permissions associated with key.")
    val permissions: Set<Permission>,

    @ApiModelProperty("A project ID for the ApiKey.", hidden = true)
    val projectId: UUID? = null
)

/**
 * The minimum properties needed for a valid API signing key.
 */
@ApiModel("SigningApiKey", description = "The attributes required to sign JWT requests.")
class SigningApiKey(
    @ApiModelProperty("The access Key")
    val accessKey: String,

    @ApiModelProperty("A shared key used to sign API requests.")
    val secretKey: String
)

@Entity
@Table(name = "api_key")
@ApiModel("ApiKey", description = "An API key allows remote users to access ZMLP resources.")
class ApiKey(

    @Id
    @Column(name = "pk_api_key")
    @ApiModelProperty("The unique ID of the ApiKey")
    val id: UUID,

    @Column(name = "project_id", nullable = false)
    @ApiModelProperty("The Project ID the ApiKey belongs in.")
    val projectId: UUID,

    @Column(name = "access_key", nullable = false)
    @ApiModelProperty("Uniquely identifies account.")
    val accessKey: String,

    @Column(name = "secret_key", nullable = false)
    @ApiModelProperty("A secret key used to sign API requests.")
    val secretKey: String,

    @Column(name = "name", nullable = false)
    @ApiModelProperty("A unique name for the key.")
    val name: String,

    @Column(name = "permissions", nullable = false)
    @Convert(converter = StringSetConverter::class)
    @ApiModelProperty("The permissions or roles for the ApiKey")
    val permissions: Set<String>
) {
    @JsonIgnore
    fun getGrantedAuthorities(): List<GrantedAuthority> {
        return if (permissions.isNullOrEmpty()) {
            listOf()
        } else {
            permissions.map { SimpleGrantedAuthority(it) }
        }
    }

    @JsonIgnore
    fun getJwtToken(timeout: Int = 60, projId: UUID? = null): String {
        val algo = Algorithm.HMAC512(secretKey)
        val spec = JWT.create().withIssuer("zmlp")
            .withClaim("accessKey", accessKey)

        if (projId != null) {
            spec.withClaim("projectId", projId.toString())
        }

        if (timeout > 0) {
            val c = Calendar.getInstance()
            c.time = Date()
            c.add(Calendar.SECOND, timeout)
            spec.withExpiresAt(c.time)
        }
        return spec.sign(algo)
    }

    @JsonIgnore
    fun getMinimalApiKey(): SigningApiKey {
        return SigningApiKey(accessKey, secretKey)
    }

    @JsonIgnore
    fun getZmlpActor(): ZmlpActor {
        return ZmlpActor(id, projectId, name, permissions.map { Permission.valueOf(it) }.toSet())
    }

    override fun toString(): String {
        return "ApiKey(Id=$id, name=$name projectId=$projectId)"
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

/**
 * Used for getting a filtered list of API keys.
 */
@ApiModel("Api Key Filter", description = "Search filter for finding API keys.")
class ApiKeyFilter(

    /**
     * A list of unique ApiKey  IDs.
     */
    @ApiModelProperty("The ApiKey IDs to match.")
    val ids: List<UUID>? = null,

    /**
     * A list of unqiue Project ids
     */
    @ApiModelProperty("The project IDs to match")
    val projectIds: List<UUID>? = null,

    /**
     * A list of unique names.
     */
    @ApiModelProperty("The key names to match")
    val names: List<String>? = null
)
