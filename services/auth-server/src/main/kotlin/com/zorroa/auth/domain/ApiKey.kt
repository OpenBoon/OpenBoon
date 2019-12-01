package com.zorroa.auth.domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.Base64
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import javax.persistence.AttributeConverter
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Converter
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@ApiModel("ApiKey Spec", description = "The attributes required to create a new API key.")
class ApiKeySpec(
    @ApiModelProperty("A unique name or label for the key.")
    val name: String,

    @ApiModelProperty("The project ID of the ApiKey")
    val projectId: UUID,

    @ApiModelProperty("A list of permissions associated with key.")
    val permissions: List<String>
)

/**
 * The minimum properties needed for a valid API signing key.
 */
@ApiModel("SigningApiKey", description = "The attributes required to sign JWT requests.")
class SigningApiKey(
    @ApiModelProperty("The unique ID of the ApiKey")
    val keyId: UUID,

    @ApiModelProperty("The project ID of the ApiKey")
    val projectId: UUID,

    @ApiModelProperty("A shared key used to sign API requests.")
    val sharedKey: String
)

/**
 * The minimal properties for a ZMLP user.
 */
@ApiModel("AuthenticatedApiKey", description = "An authenticated ApiKey")
class ZmlpActor(

    @ApiModelProperty("The unique ID of the ApiKey")
    val keyId: UUID,

    @ApiModelProperty("The project ID of the ApiKey")
    val projectId: UUID,

    @ApiModelProperty("A unique name or label.")
    val name: String,

    @ApiModelProperty("A list of permissions associated with key.")
    val permissions: List<String>
)


@Entity
@Table(name = "api_key")
@ApiModel("ApiKey", description = "An API key allows remote users to acccess ZMLP resources.")
class ApiKey(

    @Id
    @Column(name = "pk_api_key")
    @ApiModelProperty("The unique ID of the ApiKey")
    val keyId: UUID,

    @Column(name = "project_id", nullable = false)
    @ApiModelProperty("The Project ID the ApiKey belongs in.")
    val projectId: UUID,

    @Column(name = "shared_key", nullable = false)
    @ApiModelProperty("A shared key used to sign API requests.")
    val sharedKey: String,

    @Column(name = "name", nullable = false)
    @ApiModelProperty("A unique name for the key.")
    val name: String,

    @Column(name = "permissions", nullable = false)
    @Convert(converter = StringListConverter::class)
    @ApiModelProperty("The permissions or roles for the ApiKey")
    val permissions: List<String>
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
        val algo = Algorithm.HMAC512(sharedKey)
        val spec = JWT.create().withIssuer("zorroa")
            .withClaim("keyId", keyId.toString())
            .withClaim("projectId", (projId ?: projectId).toString())

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
        return SigningApiKey(keyId, projectId, sharedKey)
    }

    @JsonIgnore
    fun getZmlpActor(): ZmlpActor {
        return ZmlpActor(keyId, projectId, name, permissions)
    }

    override fun toString(): String {
        return "ApiKey(keyId=$keyId, projectId=$projectId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApiKey

        if (keyId != other.keyId) return false
        if (projectId != other.projectId) return false
        if (sharedKey != other.sharedKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyId.hashCode()
        result = 31 * result + projectId.hashCode()
        result = 31 * result + sharedKey.hashCode()
        return result
    }
}

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {

    override fun convertToDatabaseColumn(list: List<String>): String {
        return list.joinToString(",")
    }

    override fun convertToEntityAttribute(joined: String): List<String> {
        return joined.split(",").map { it.trim() }
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
    val keyIds: List<UUID>? = null,

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

/**
 * Generates API signing keys.
 */
object KeyGenerator {
    fun generate(): String {
        val random = ThreadLocalRandom.current()
        val r = ByteArray(64)
        random.nextBytes(r)
        return Base64.getUrlEncoder().encodeToString(r).trimEnd('=')
    }
}
