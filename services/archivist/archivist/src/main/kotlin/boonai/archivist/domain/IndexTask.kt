package boonai.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.elasticsearch.client.tasks.GetTaskRequest
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

enum class IndexTaskType {
    /**
     * A reindex tasks moves data from 1 index to the other.
     */
    REINDEX,

    /**
     * The update task does mass async asset updates.
     */
    UPDATE
}

enum class IndexTaskState {
    /**
     * The task is executing.
     */
    RUNNING,

    /**
     * The task is finished.
     */
    FINISHED
}

@Entity
@Table(name = "index_task")
@ApiModel("Index Task", description = "A long running task modifying a project index.")
class IndexTask(

    @Id
    @Column(name = "pk_index_task")
    @ApiModelProperty("The Unique ID of the migration")
    val id: UUID,

    @Column(name = "pk_project")
    @ApiModelProperty("The ID of the project")
    val projectId: UUID,

    @Column(name = "pk_index_route_src", nullable = false)
    @ApiModelProperty("The source index.")
    val srcIndexRouteId: UUID,

    @Column(name = "pk_index_route_dst")
    @ApiModelProperty("The destination index, if any.")
    val dstIndexRouteId: UUID?,

    @Column(name = "str_name", nullable = false)
    @ApiModelProperty("The name job")
    val name: String,

    @Column(name = "int_type")
    @ApiModelProperty("The state of the migration")
    val type: IndexTaskType,

    @Column(name = "int_state")
    @ApiModelProperty("The state of the migration")
    val state: IndexTaskState,

    @Column(name = "str_es_task_id")
    @ApiModelProperty("The name of the ES index task")
    val esTaskId: String,

    @Column(name = "time_created")
    @ApiModelProperty("The time the Project was created..")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the Project was modified.")
    val timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The actor which created this Project")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The actor that last made the last modification the project.")
    val actorModified: String
) {
    fun buildGetTaskRequest(): GetTaskRequest {
        val items = esTaskId.split(':')
        val nodeId = items[0]
        val taskNum = items[1].toLong()
        return GetTaskRequest(nodeId, taskNum)
    }
}

@ApiModel("IndexToIndexMigrationSpec", description = "Create a new index migration.")
class IndexToIndexMigrationSpec(

    @ApiModelProperty("The src index route.")
    val srcIndexRouteId: UUID,

    @ApiModelProperty("The dst index route")
    val dstIndexRouteId: UUID
)

@ApiModel(
    "ProjectIndexMigrationSpec",
    description = "Migrate a project to new major version of it's index mapping."
)
class ProjectIndexMigrationSpec(

    @ApiModelProperty("The index mapping name, usually english_strict")
    val mapping: String,

    @ApiModelProperty("The index mapping major version.")
    val majorVer: Int,

    @ApiModelProperty("The size of the project, default to current size.")
    val size: ProjectSize? = null,

    @ApiModelProperty("The cluster for the project, defaults to same cluster.")
    val clusterId: UUID? = null
)
