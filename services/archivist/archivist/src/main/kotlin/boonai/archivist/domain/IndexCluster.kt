package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import boonai.archivist.repository.KDaoFilter
import boonai.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

enum class IndexClusterState {
    /**
     * Cluster is down.
     */
    DOWN,
    /**
     * Cluster is up and running.
     */
    UP,
    /**
     * The state is currently unknown.
     */
    PENDING
}

/**
 * An IndexClusterSpec are the properties needed to make an [IndexCluster]
 *
 * @property url The full cluster URL
 * @property autoPool True if the cluster should be auto-selected for new projects.
 */
class IndexClusterSpec(
    val url: String,
    val autoPool: Boolean
)

/**
 * An IndexCluster represents an ElasticSearch cluster.
 *
 * @property id The unique ID of the cluster.
 * @property url The base url of the cluster.
 * @property autoPool If the cluster is in the auto selection pool.
 * @property state The state of the cluster.
 * @property createdTime The timestamp the cluster was created on.
 * @property modifiedTime The timestamp of the last time the cluster was modified.
 * @property lastPingTime The timestamp of the last cluster ping.
 * @property attrs Arbitrary cluster attributes gathered from a ping.
 */
class IndexCluster(
    @ApiModelProperty("The unique ID of the cluster.")
    val id: UUID,
    @ApiModelProperty("The base url of the cluster.")
    val url: String,
    @ApiModelProperty("If the cluster is in the auto selection pool.")
    val autoPool: Boolean,
    @ApiModelProperty("The state of the cluster.")
    val state: IndexClusterState,
    @ApiModelProperty("The timestamp the cluster was created on.")
    val createdTime: Long,
    @ApiModelProperty("The timestamp of the last time the cluster was modified.")
    val modifiedTime: Long,
    @ApiModelProperty("The timestamp of the last cluster ping.")
    val lastPingTime: Long,
    @ApiModelProperty("Arbitrary cluster attributes gathered from a ping.")
    val attrs: Map<String, Any>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IndexCluster

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "IndexCluster(id=$id, url='$url')"
    }
}

/**
 * A class for filtering [IndexCluster]s
 */
class IndexClusterFilter(
    val ids: List<UUID>? = null,
    val urls: List<String>? = null,
    val autoPool: Boolean? = null
) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "id" to "index_route.pk_index_route",
            "url" to "index_cluster.str_url",
            "timeCreated" to "index_cluster.time_created",
            "timeModified" to "index_cluster.time_modified"
        )

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("timeCreated:desc")
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("index_cluster.pk_index_route", it.size))
            addToValues(it)
        }

        urls?.let {
            addToWhere(JdbcUtils.inClause("index_cluster.str_url", it.size))
            addToValues(it)
        }

        autoPool?.let {
            addToWhere("index_cluster.bool_autopool=?")
            addToValues(it)
        }
    }
}
