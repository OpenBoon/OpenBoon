package boonai.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

enum class BoonLibEntity {
    Dataset
}

enum class BoonLibEntityType(val entity: BoonLibEntity) {
    Classification(BoonLibEntity.Dataset),
    Detection(BoonLibEntity.Dataset),
    FaceRecognition(BoonLibEntity.Dataset);

    fun isCompatible(entity: BoonLibEntity): Boolean {
        return this.entity == entity
    }
}

enum class BoonLibState {
    EMPTY,
    READY
}

enum class LicenseType(name: String) {
    CC0("CC0 1.0 Public Domain")
}

/**
 * Properties required to create a BoonLib
 */
class BoonLibSpec(
    var name: String,
    val entity: BoonLibEntity,
    val entityType: BoonLibEntityType,
    var license: LicenseType,
    var description: String,
    var entityId: UUID? = null

)

/**
 * Properties for importing a BoonLib.
 */
class BoonLibImportRequest(
    val boonlibId: UUID
)

class BoonLibImportResponse(
    val count: Int,
    val tookMillis: Long
)

@Entity
@Table(name = "boonlib")
@ApiModel("BoonLib", description = "BoonLibs are importable items.")
class BoonLib(

    @Id
    @Column(name = "pk_boonlib")
    @ApiModelProperty("The unique ID of the BoonLib")
    val id: UUID,

    @Column(name = "name")
    var name: String,

    @Column(name = "entity")
    val entity: BoonLibEntity,

    @Column(name = "entity_type")
    val entityType: BoonLibEntityType,

    @Column(name = "descr")
    var description: String,

    @Column(name = "license")
    var license: LicenseType,

    @Column(name = "state")
    var state: BoonLibState,

    @Column(name = "time_created")
    @ApiModelProperty("The time the Model was created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the Model was modified.")
    var timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The key which created this Model")
    val actorCreated: String,

    @Column(name = "actor_modified")
    var actorModified: String
) {
    fun isCompatible(obj: Any): Boolean {
        return if (obj is Dataset) {
            when (entity) {
                BoonLibEntity.Dataset -> {
                    obj.type == DatasetType.Classification
                }
            }
        } else {
            false
        }
    }

    fun checkCompatible(obj: Any) {
        if (!isCompatible(obj)) {
            throw IllegalArgumentException("The '$name BoonLib is not compatible with the target object")
        }
    }
}
