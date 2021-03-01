package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.readValue
import boonai.archivist.security.getZmlpActor
import boonai.common.util.Json
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.lang.IllegalArgumentException
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

enum class CredentialsType(vararg val keys: String) {
    GCP(
        "type", "project_id", "private_key_id", "private_key", "client_email",
        "client_id", "auth_uri", "token_uri", "auth_provider_x509_cert_url",
        "client_x509_cert_url"
    ),
    AWS("aws_access_key_id", "aws_secret_access_key"),
    AZURE("connection_string");

    fun validate(blob: String) {
        val parsed = Json.Mapper.readValue<Map<String, Any>>(blob)
        this.keys.forEach {
            if (!parsed.containsKey(it)) {
                throw IllegalArgumentException("Credentials blob is missing $it property")
            }
        }
    }
}

@ApiModel(
    "Credentials Spec",
    description = "Field necessary for creating a new credentials blob."
)
class CredentialsSpec(

    @ApiModelProperty("A unique name for the credentials.")
    val name: String,

    @ApiModelProperty("The credentials type")
    val type: CredentialsType,

    @ApiModelProperty("The credentials blob, a json formatted string")
    val blob: String
)

@Entity
@Table(name = "credentials")
@ApiModel("Credentials", description = "Credentials for accessing storage in various cloud services.")
class Credentials(

    @Id
    @Column(name = "pk_credentials")
    @ApiModelProperty("The unique Id of the credentials.")
    val id: UUID,

    @Column(name = "pk_project")
    @ApiModelProperty("The projectId the credentials belongs to.")
    val projectId: UUID,

    @Column(name = "str_name")
    @ApiModelProperty("A unique name for the credentials")
    val name: String,

    @Column(name = "int_type")
    @ApiModelProperty("The type of credentials.")
    val type: CredentialsType,

    @Column(name = "time_created")
    @ApiModelProperty("The time the credentials were created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time credentials were modified.")
    val timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The actor which created this Project")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The actor that last made the last modification the project.")
    val actorModified: String
) {
    @JsonIgnore
    fun getUpdated(update: CredentialsUpdate): Credentials {
        return Credentials(
            id, projectId, update.name, type,
            timeCreated, System.currentTimeMillis(),
            actorCreated, getZmlpActor().toString()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Credentials) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        const val CRYPT_VARIANCE = 131
    }
}

@ApiModel(
    "Credentials Update",
    description = "The fields that can be updated by credentials update.."
)
class CredentialsUpdate(

    @ApiModelProperty("A name for the credentials")
    var name: String,

    @ApiModelProperty("An arbitrary credentials blob.  Only provide this if you need it to be changed.")
    var blob: String? = null
)
