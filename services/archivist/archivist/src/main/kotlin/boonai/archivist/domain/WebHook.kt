package boonai.archivist.domain

import boonai.archivist.repository.KDaoFilter
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.Splitter
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.AttributeConverter
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Converter
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

enum class TriggerType {
    @ApiModelProperty("Emitted on the first time the Asset is Analyzed")
    AssetAnalyzed,
    @ApiModelProperty("Emitted on manual editing or reprocessing of Asset")
    AssetModified
}

@Converter
class TriggerConverter : AttributeConverter<Array<TriggerType>, String> {
    override fun convertToDatabaseColumn(attribute: Array<TriggerType>): String {
        return attribute.map { it.ordinal }.joinToString(",")
    }

    override fun convertToEntityAttribute(dbData: String): Array<TriggerType> {
        val triggers = Splitter.on(',').trimResults().omitEmptyStrings().split(dbData)
        return triggers.map { TriggerType.values()[it.toInt()] }.toTypedArray()
    }
}

class WebHookSpec(

    @ApiModelProperty("The remote URL for the web hook.")
    val url: String,

    @ApiModelProperty("The secret token used by the webhook server to validate the request")
    val secretKey: String,

    @ApiModelProperty("The triggers the webhook should fire on.")
    val triggers: Array<TriggerType>,

    @ApiModelProperty("Determines if the webhook is active or not.")
    var active: Boolean = true
)

class WebHookUpdate(

    @ApiModelProperty("The remote URL for the web hook.")
    val url: String,

    @ApiModelProperty("The secret token used by the webhook server to validate the request")
    val secretKey: String,

    @ApiModelProperty("The triggers the webhook should fire on.")
    val triggers: Array<TriggerType>,

    @ApiModelProperty("Determines if the webhook is active or not.")
    val active: Boolean
)

class WebHookPatch(

    @ApiModelProperty("The remote URL for the web hook.")
    val url: String? = null,

    @ApiModelProperty("The secret token used by the webhook server to validate the request")
    val secretKey: String? = null,

    @ApiModelProperty("The triggers the webhook should fire on.")
    val triggers: Array<TriggerType>? = null,

    @ApiModelProperty("Determines if the hook is enabled or not.")
    val active: Boolean? = null
)

@Entity
@Table(name = "webhook")
@ApiModel("WebHook", description = "WebHooks ")
class WebHook(

    @Id
    @Column(name = "pk_webhook")
    @ApiModelProperty("The unique ID of the WebHook")
    val id: UUID,

    @Column(name = "pk_project")
    val projectId: UUID,

    @Column(name = "url")
    val url: String,

    @Column(name = "secret_key")
    val secretKey: String,

    @Convert(converter = TriggerConverter::class)
    @Column(name = "triggers")
    val triggers: Array<TriggerType>,

    @Column(name = "active")
    val active: Boolean,

    @Column(name = "time_created")
    @ApiModelProperty("The time the WebHook was created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the WebHook was modified.")
    val timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The key which created this WebHook")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The key that last made the last modification to this WebHook")
    val actorModified: String
)

class WebHookFilter constructor(
    val ids: List<UUID>? = null,
    val urls: List<String>? = null
) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "id" to "webhook.pk_webhook",
            "url" to "webhook.url",
            "timeCreated" to "webhook.time_created",
            "timeModified" to "webhook.time_modified",
            "triggers" to "webhook.triggers",
            "active" to "webhook.active"
        )

    @JsonIgnore
    override fun build() {
        addToWhere("webhook.pk_project=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("webhook.pk_webhook", it.size))
            addToValues(it)
        }

        urls?.let {
            addToWhere(JdbcUtils.inClause("webhook.url", it.size))
            addToValues(it)
        }
    }
}
