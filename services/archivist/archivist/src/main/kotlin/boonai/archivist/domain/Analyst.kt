package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import boonai.archivist.repository.KDaoFilter
import boonai.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

enum class AnalystState {
    Down,
    Up
}

enum class LockState {
    Unlocked,
    Locked
}
class AnalystSpec(
    val totalRamMb: Int,
    val freeRamMb: Int,
    val freeDiskMb: Int,
    val load: Float,
    val version: String,
    val taskId: UUID? = null
) {
    @JsonIgnore
    var endpoint: String? = null
}

@ApiModel("Analyst", description = "Describes an Analyst worker node.")
class Analyst(
    @ApiModelProperty("UUID of the Analyst.")
    val id: UUID,

    @ApiModelProperty("UUID of Task currently running on the Analyst.")
    val taskId: UUID?,

    @ApiModelProperty("URL where the Analyst can be reached.")
    val endpoint: String,

    @ApiModelProperty("Total RAM Analyst can use in mb.")
    val totalRamMb: Int,

    @ApiModelProperty("Amount RAM free on the Analyst in mb.")
    val freeRamMb: Int,

    @ApiModelProperty("Free disk sapce on the Analyst in mb.")
    val freeDiskMb: Int,

    @ApiModelProperty("Current CPU load on the Analyst.")
    val load: Float,

    @ApiModelProperty("Time it took to ping the Analyst.")
    val timePing: Long,

    @ApiModelProperty("Datetime the Analyst was created.")
    val timeCreated: Long,

    @ApiModelProperty("Current state of the Analyst")
    val state: AnalystState,

    @ApiModelProperty("Lock status of the Analyst", allowableValues = "locked,unlocked")
    val lock: LockState,

    @ApiModelProperty("The version of the analyst.")
    val version: String

) {
    override fun toString(): String {
        return "<Analyst id='$id' endpoint='$endpoint'>"
    }
}

@ApiModel("Analyst Filter", description = "Search filter for finding Analysts.")
data class AnalystFilter(

    @ApiModelProperty("Analyst UUIDs.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Analyst states.", allowableValues = "up,down")
    val states: List<AnalystState>? = null,

    @ApiModelProperty("Task UUIDs that are running on Analysts.")
    val taskIds: List<UUID>? = null,

    @ApiModelProperty("Analyst lock states.", allowableValues = "locked,unlocked")
    val lockStates: List<LockState>? = null,

    @ApiModelProperty("URL endpoints of Analysts.")
    val endpoints: List<String>? = null
) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "id" to "analyst.pk_analyst",
        "load" to "analyst.flt_load",
        "endpoint" to "analyst.str_endpoint",
        "lock" to "analyst.int_lock_state",
        "timePing" to "analyst.time_ping"
    )

    @JsonIgnore
    override fun build() {

        ids?.let {
            addToWhere(JdbcUtils.inClause("analyst.pk_analyst", it.size))
            addToValues(it)
        }

        endpoints?.let {
            addToWhere(JdbcUtils.inClause("analyst.str_endpoint", it.size))
            addToValues(it)
        }

        states?.let {
            addToWhere(JdbcUtils.inClause("analyst.int_state", it.size))
            addToValues(it.map { s -> s.ordinal })
        }

        lockStates?.let {
            addToWhere(JdbcUtils.inClause("analyst.int_lock_state", it.size))
            addToValues(it.map { s -> s.ordinal })
        }

        taskIds?.let {
            addToWhere(JdbcUtils.inClause("analyst.pk_task", it.size))
            addToValues(it)
        }
    }
}
