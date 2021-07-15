package boonai.archivist.domain

import boonai.archivist.repository.KDaoFilter
import boonai.archivist.security.hasPermission
import boonai.archivist.util.JdbcUtils
import boonai.common.apikey.Permission
import com.fasterxml.jackson.annotation.JsonIgnore
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

enum class BoonLibState {
    PROCESSING,
    READY
}

/**
 * Properties required to create a BoonLib
 */
class BoonLibSpec(
    var name: String,
    var description: String,
    val entity: BoonLibEntity,
    val entityId: UUID
)

/**
 * Optional properties required to update a BoonLib
 */
class BoonLibUpdateSpec(
    var name: String? = null,
    var description: String? = null
)

/**
 * Properties for importing a BoonLib.
 */
class BoonLibImportRequest(
    val boonlibId: UUID
)

/**
 * BoonLib import result.
 */
class BoonLibImportResponse(
    val count: Int,
    val tookMillis: Long
)

/**
 *  Filter parameters to search a BoonLib
 */
class BoonLibFilter(

    val ids: List<UUID>? = null,
    val names: List<String>? = null,
    val entities: List<BoonLibEntity>? = null,
    val entityTypes: List<String>? = null,

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "id" to "boonlib.pk_boonlib",
            "name" to "boonlib.name",
            "entity" to "boonlib.entity",
            "entityType" to "boonlib.entity_type",
            "description" to "boonlib.descr",
            "license" to "boonlib.license",
            "state" to "boonlib.state",
            "timeCreated" to "boonlib.time_created",
            "timeModified" to "boonlib.time_modified",
            "actorCreated" to "boonlib.actor_created",
            "actorModified" to "boonlib.actor_modified",
        )

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("timeCreated:desc")
        }

        if (!hasPermission(Permission.SystemManage)) {
            addToWhere("boonlib.state=?")
            addToValues(BoonLibState.READY.ordinal)
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("boonlib.pk_boonlib", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("boonlib.name", it.size))
            addToValues(it)
        }

        entities?.let {
            addToWhere(JdbcUtils.inClause("boonlib.entity", it.size))
            addToValues(it.map { s -> s.ordinal })
        }

        entityTypes?.let {
            addToWhere(JdbcUtils.inClause("boonlib.entity_type", it.size))
            addToValues(it)
        }
    }
}

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
    val entityType: String,

    @Column(name = "descr")
    var description: String,

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

    fun checkCompatible(obj: Any) {
        if (!isCompatible(obj)) {
            throw IllegalArgumentException("The object type is not compatible with this BoonLib.")
        }
    }

    fun isCompatible(obj: Any): Boolean {
        if (obj is Dataset) {
            return obj.type.name.lowercase() == entityType.lowercase()
        }
        return false
    }
}
