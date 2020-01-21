package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import com.zorroa.archivist.security.getZmlpActor
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.springframework.data.annotation.LastModifiedDate
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version

@Entity
@Table(name = "module")
@ApiModel("PipelineMod", description = "A ZMLP Pipeline Modifier")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
class PipelineMod(

    @Id
    @Column(name = "pk_module")
    @ApiModelProperty("The Unique ID of the module")
    val id: UUID,

    @Column(name = "str_name")
    @ApiModelProperty("A unique name of the module")
    val name: String,

    @Column(name = "str_description")
    @ApiModelProperty("A description of what the module does")
    val description: String,

    @Column(name = "bool_restricted")
    @ApiModelProperty("This module is only available if granted to a project.")
    val restricted: Boolean,

    @Type(type = "jsonb")
    @Column(name = "json_ops", columnDefinition = "JSON")
    @ApiModelProperty("A list of operations to apply to the pipeline")
    val ops: List<ModOp>,

    @Column(name = "time_created", updatable = false)
    @ApiModelProperty("The time the Pipeline Mod was created..")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the Pipeline Mod was modified.")
    @LastModifiedDate
    @Version
    var timeModified: Long,

    @Column(name = "actor_created", updatable = false)
    @ApiModelProperty("The actor which created this Pipeline Mod")
    var actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The actor that last made the last modification the Pipeline Mod.")
    var actorModified: String
) {
    fun getUpdated(update: PipelineModUpdate): PipelineMod {
        return PipelineMod(id,
            update.name,
            update.description,
            update.restricted,
            update.ops,
            timeCreated, System.currentTimeMillis(), actorCreated,
            getZmlpActor().toString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PipelineMod) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@ApiModel("PipelineModUpdate", description = "All the Pipeline mod fields that can be updated.")
class PipelineModUpdate(

    @ApiModelProperty("A unique name of the module")
    val name: String,

    @ApiModelProperty("A description of what the module does")
    val description: String,

    @ApiModelProperty("This module is only available if granted to a project.")
    val restricted: Boolean,

    @ApiModelProperty("A list of operations to apply to the pipeline")
    val ops: List<ModOp>
)

@ApiModel("PipelineModSpec", description = "Fields for creating a new Pipeline Mod")
class PipelineModSpec(

    @ApiModelProperty("A unique name of the module")
    val name: String,

    @ApiModelProperty("A description of what the module does")
    val description: String,

    @ApiModelProperty("A list of operations to apply to the pipeline")
    val ops: List<ModOp>,

    @ApiModelProperty("This module is only available if granted to a project.")
    val restricted: Boolean = false
)

@ApiModel("Pipeline ModOpType", description = "Types of operations a Pipeline Mod can make.")
enum class ModOpType() {

    @ApiModelProperty("Set arguments on a ProcessorRef")
    SET_ARGS,
    @ApiModelProperty("Append to the Pipeline")
    APPEND,
    @ApiModelProperty("Prepend to the Pipeline")
    PREPEND,
    @ApiModelProperty("Add processors before a matched processor")
    ADD_BEFORE,
    @ApiModelProperty("Add processors after a matched processor")
    ADD_AFTER,
    @ApiModelProperty("Replace matched processor")
    REPLACE,
    @ApiModelProperty("Remove matched processors")
    REMOVE,
    @ApiModelProperty("Append processors as far back as possible.")
    LAST
}

@ApiModel("Pipeline Mod Operation", description = "An operation to apply to a Pipeline")
class ModOp(
    @ApiModelProperty("The opType")
    val type: ModOpType,
    @ApiModelProperty("The data necesssary for OpType, which depends on the type")
    val apply: Any?,
    @ApiModelProperty("Matchers which determine if an Op is applied.")
    var filter: OpFilter? = null,
    @ApiModelProperty("The max number of times the Op should be applied")
    val maxApplyCount: Int = 1
) {
    /**
     * This is incremented every time an Op is applied.
     */
    @JsonIgnore
    var applyCount: Int = 0
}

@ApiModel("Filter Type", description = "The type of Filter.")
enum class OpFilterType {
    @ApiModelProperty("Match processor names with a regular expression.")
    REGEX,
    @ApiModelProperty("Match processor names with a substr match.")
    SUBSTR,
    @ApiModelProperty("Match processor names with equals.")
    EQUAL,
    @ApiModelProperty("Match processor names that don't match this regex.")
    NOT_REGEX,
    @ApiModelProperty("Match processor names that don't match this substr.")
    NOT_SUBSTR
}

@ApiModel("Op Filter", description = "Matches processor names and args on a ProcessorRef")
class OpFilter(
    @ApiModelProperty("The Filter type")
    val type: OpFilterType,
    @ApiModelProperty("The text to Match")
    val processor: String?
)
