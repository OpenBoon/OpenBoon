package boonai.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

enum class AsyncProcessType {
    DELETE_PROJECT_STORAGE,
    DELETE_PROJECT_INDEXES
}

enum class AsyncProcessState {
    PENDING,
    RUNNING,
    SUCCESS,
    ERROR
}

class AsyncProcessSpec(
    val projectId: UUID,
    val description: String,
    val type: AsyncProcessType
)

@Entity
@Table(name = "process")
@ApiModel("AsyncProcess", description = "AsyncProcess are long running processes in the archivist.")
class AsyncProcess(

    @Id
    @Column(name = "pk_process")
    @ApiModelProperty("The Process ID")
    val id: UUID,

    @Column(name = "pk_project")
    val projectId: UUID,

    @Column(name = "str_descr")
    var description: String,

    @Column(name = "int_type")
    val type: AsyncProcessType,

    @Column(name = "int_state")
    val state: AsyncProcessState,

    @Column(name = "time_created")
    @ApiModelProperty("The time the AsyncProcess was created.")
    val timeCreated: Long,

    @Column(name = "time_started")
    @ApiModelProperty("The time the AsyncProcess was started.")
    val timeStarted: Long,

    @Column(name = "time_stopped")
    @ApiModelProperty("The time the AsyncProcess was stopped.")
    val timeStopped: Long,

    @Column(name = "time_refresh")
    @ApiModelProperty("The time the AsyncProcess was refreshed")
    val timeRefresh: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The actor which created this Field")
    val actorCreated: String
)
