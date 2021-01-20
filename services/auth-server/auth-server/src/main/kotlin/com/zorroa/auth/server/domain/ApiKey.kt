package com.zorroa.auth.server.domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.auth.server.repository.AbstractJpaFilter
import com.zorroa.auth.server.repository.EncryptedConverter
import com.zorroa.auth.server.security.getProjectId
import com.zorroa.zmlp.apikey.Permission
import com.zorroa.zmlp.apikey.ZmlpActor
import com.zorroa.zmlp.service.jpa.StringSetConverter
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
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

@ApiModel("ApiKey Spec", description = "The attributes required to create a new API key.")
class ApiKeySpec(
    @ApiModelProperty("A unique name or label for the key.")
    val name: String,

    @ApiModelProperty("A list of permissions associated with key.")
    val permissions: Set<Permission>,

    @JsonIgnore
    @ApiModelProperty("An optional project Id override, not available via REST.", hidden = true)
    val projectId: UUID? = null,

    @ApiModelProperty("Key enabled status")
    val enabled: Boolean = true,

    @ApiModelProperty("Indicates that is a hidden key", hidden = true)
    val hidden: Boolean = false
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
    @Convert(converter = EncryptedConverter::class)
    @JsonIgnore
    val secretKey: String,

    @Column(name = "name", nullable = false)
    @ApiModelProperty("A unique name for the key.")
    val name: String,

    @Column(name = "permissions", nullable = false)
    @Convert(converter = StringSetConverter::class)
    @ApiModelProperty("The permissions or roles for the ApiKey")
    val permissions: Set<String>,

    @Column(name = "time_created", nullable = false)
    @ApiModelProperty("The time the key was created.")
    val timeCreated: Long,

    @Column(name = "time_modified", nullable = false)
    @ApiModelProperty("The time the key was modified.")
    val timeModified: Long,

    @Column(name = "actor_created", nullable = false)
    @ApiModelProperty("The actor that created the key.")
    val actorCreated: String,

    @Column(name = "actor_modified", nullable = false)
    @ApiModelProperty("The actor that modified the key.")
    val actorModified: String,

    @Column(name = "enabled", nullable = false)
    @ApiModelProperty("True if the Key is enabled")
    val enabled: Boolean,

    @Column(name = "system_key")
    @ApiModelProperty("Indicates that is a System Key", hidden = true)
    val systemKey: Boolean,

    @Column(name = "hidden")
    @ApiModelProperty("Indicates that is a hidden key", hidden = true)
    val hidden: Boolean

) {

    @JsonIgnore
    fun getValidationKey(): ValidationKey {
        return ValidationKey(id, projectId, accessKey, secretKey, name, permissions)
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

    companion object {
        /**
         * Adds variance to the key used to encrypt this data.  Each form
         * of data gets is own variance integer.
         */
        const val CRYPT_VARIANCE = 1023
    }
}

/**
 * A minimal key used for JWT validation and creation of the [ZmlpActor]
 *
 * @property id The key id.
 * @property projectId The project Id.
 * @property accessKey The access key.
 * @property secretKey The secret key.
 * @property name The name of the key.
 * @property permissions The permissions for the key.
 *
 */
class ValidationKey(
    val id: UUID,
    val projectId: UUID,
    val accessKey: String,
    val secretKey: String,
    val name: String,
    val permissions: Set<String>
) {

    /**
     * Return the permissions as [GrantedAuthority]
     */
    fun getGrantedAuthorities(): List<GrantedAuthority> {
        return if (permissions.isNullOrEmpty()) {
            listOf()
        } else {
            permissions.map { SimpleGrantedAuthority(it) }
        }
    }

    /**
     * Return the [ZmlpActor] for this key.  Optionally override the project Id.
     */
    fun getZmlpActor(projectId: UUID? = null, attrs: Map<String, String>? = null): ZmlpActor {

        return ZmlpActor(
            id,
            projectId ?: this.projectId,
            name,
            permissions.map { Permission.valueOf(it) }.toSet(),
            attrs
        )
    }

    /**
     * Only used in tests.
     */
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
     * A list of unique names.
     */
    @ApiModelProperty("The key names to match")
    val names: List<String>? = null,

    /**
     * A list of unique names.
     */
    @ApiModelProperty("Key name prefixes to match.")
    val namePrefixes: List<String>? = null,

    @ApiModelProperty("Set to true to show system keys.", hidden = true)
    val systemKey: Boolean? = null

) : AbstractJpaFilter<ApiKey>() {

    override fun buildWhereClause(root: Root<ApiKey>, cb: CriteriaBuilder): Array<Predicate> {
        val where = mutableListOf<Predicate>()

        where.add(cb.equal(root.get<UUID>("projectId"), getProjectId()))
        where.add(cb.equal(root.get<Boolean>("hidden"), false))

        ids?.let {
            val ic: CriteriaBuilder.In<UUID> = cb.`in`(root.get("id"))
            it.forEach { v ->
                ic.value(v)
            }
            where.add(ic)
        }

        names?.let {
            val ic: CriteriaBuilder.In<String> = cb.`in`(root.get("name"))
            it.forEach { v ->
                ic.value(v)
            }
            where.add(ic)
        }

        namePrefixes?.let {
            val matches = it.map { v ->
                cb.like(root.get("name"), "$v%")
            }
            where.add(cb.or(*matches.toTypedArray()))
        }

        systemKey?.let {
            where.add(cb.equal(root.get<Boolean>("systemKey"), it))
        }

        return where.toTypedArray()
    }

    override val sortFields: Set<String>
        get() = setOf("id", "name", "timeCreated")
}
