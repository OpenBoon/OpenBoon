package boonai.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

enum class AutomlSessionState {
    TRAINING,
    FINISHED,
    ERROR
}

class AutomlSessionSpec(
    val automlDataSet: String,
    val automlTrainingJob: String
)

@Entity
@Table(name = "automl")
@ApiModel("AutoML", description = "Models are used to make predictions.")
class AutomlSession(

    @Id
    @Column(name = "pk_automl")
    @ApiModelProperty("The unique ID of the AutoML session")
    val id: UUID,

    @Column(name = "pk_model")
    @ApiModelProperty("The unique ID of the Model")
    val modelId: UUID,

    @Column(name = "pk_project")
    @ApiModelProperty("The unique ID the assoicatd project.")
    val projectId: UUID,

    @Column(name = "str_dataset")
    @ApiModelProperty("The ID of the AutoML dataset")
    val automlDataSet: String,

    @Column(name = "str_training_job")
    @ApiModelProperty("The ID of the AutoML model")
    val automlTrainingJob: String,

    @Column(name = "str_model")
    @ApiModelProperty("The ID of the AutoML model")
    val automlModel: String?,

    @Column(name = "str_error")
    @ApiModelProperty("The AutoML error, if any")
    val error: String?,

    @Column(name = "int_state")
    @ApiModelProperty("The ID of the AutoML model")
    val state: AutomlSessionState,

    @Column(name = "time_created")
    @ApiModelProperty("The time the Model was created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the Model was modified.")
    val timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The key which created this Model")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The key that last made the last modification to this Model")
    val actorModified: String
)
