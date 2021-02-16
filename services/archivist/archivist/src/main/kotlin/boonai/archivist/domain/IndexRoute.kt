package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import boonai.archivist.repository.KDaoFilter
import boonai.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * The state of an [IndexRoute] determines what index is the index
 * in use, which index is being built by some migration operation
 * and what index is ready to be deleted.  A project can only have 1
 * index in each state, i.e. there can be only 1 CURRENT index, etc.
 *
 * The reason we only allow a project to have an index in each state
 * is that we don't want more than 1 major re-indexing operatation
 * going at a time.  In the short term this solves that problem.
 *
 * In the future we could allow multiple indexes per project.
 *
 */
enum class IndexRouteState {
    /**
     * The [IndexRoute] exists and is ready to query.
     */
    READY,

    /**
     * The index is queued to be deleted.
     */
    CLOSED
}

/**
 * An IndexRoute points to a unique ES cluster and index name.
 *
 * @property id The unique ID of the index route.
 * @property projectId The unique ID of the project.
 * @property clusterId The unique ID of the cluster.
 * @property clusterUrl The URL to the ES cluster.
 * @property state The state of the index.
 * @property indexName The name of the ES index.
 * @property mapping The mapping type. This is extracted from the
 * mapping file name, not the ES type.
 * @property majorVer The major version of the mapping file.
 * @property minorVer The minor version of the mapping file in a date format.
 * @property replicas Number of index replicas.
 * @property shards Number of shards.
 * @property indexUrl The ES index URL, or the cluster URL and index name combined.
 */
@ApiModel(
    "IndexRoute",
    description = "TaskErrorEvents are emitted by the processing system if an an exception is thrown while processing"
)

class IndexRoute(
    @ApiModelProperty("The unique ID of the index route.")
    val id: UUID,
    @ApiModelProperty("The unique ID of the project.")
    val projectId: UUID,
    @ApiModelProperty("The unique ID of the cluster.")
    val clusterId: UUID,
    @ApiModelProperty("The URL to the ES cluster.")
    val clusterUrl: String,
    @ApiModelProperty("The state of the index.")
    val state: IndexRouteState,
    @ApiModelProperty("The name of the ES index.")
    val indexName: String,
    @ApiModelProperty("The mapping type. This is extracted from the mapping file name, not the ES type.")
    val mapping: String,
    @ApiModelProperty("The major version of the mapping file.")
    val majorVer: Int,
    @ApiModelProperty("The minor version of the mapping file in a date format.")
    val minorVer: Int,
    @ApiModelProperty("Number of index replicas.")
    val replicas: Int,
    @ApiModelProperty("Number of shards.")
    val shards: Int
) {
    @ApiModelProperty("The ES index URL, or the cluster URL and index name combined.")
    val indexUrl = "$clusterUrl/$indexName"

    /**
     * Return an [EsClientCacheKey] which will apply writes across all shards.
     */
    @JsonIgnore
    fun esClientCacheKey(): EsClientCacheKey {
        return EsClientCacheKey(clusterUrl, indexName, majorVer)
    }

    @JsonIgnore
    fun redisCacheKey(): String {
        return "zmlp/archivist/$projectId/index"
    }

    override fun toString(): String {
        return "IndexRoute(id=$id, indexUrl='$indexUrl')"
    }
}

@ApiModel("IndexRouteSimpleSpec", description = "Create a new index of a given size.")
class IndexRouteSimpleSpec(

    @ApiModelProperty("The new project size.")
    val size: ProjectSize,

    @ApiModelProperty("The Project Id to create the route spec under.")
    var projectId: UUID? = null
)

/**
 * The IndexRouteSpec defines all the values needed to create an index route.
 *
 * @property mapping The type of mapping (not ES object type)
 * @property majorVer The major version to use. It will be patched up to highest level.
 * @property state The state of the index route.
 * @property replicas The number of replicas there should be for each shard. Defaults to 0.
 * @property shards The number of shards in the index. Defaults to 5.
 * @property clusterId The cluster ID to use for the index.
 * @property projectId An optional projectID, not available for endpoints.
 */
@ApiModel("IndexRouteSpec", description = "The IndexRouteSpec defines all the values needed to create an index route.")
class IndexRouteSpec(
    @ApiModelProperty("The type of mapping (not ES object type)")
    var mapping: String,
    @ApiModelProperty("The major version to use. It will be patched up to highest level.")
    var majorVer: Int,
    @ApiModelProperty("The number of replicas there should be for each shard. Defaults to 1.")
    var replicas: Int = 0,
    @ApiModelProperty(" The number of shards in the index. Defaults to 5.")
    var shards: Int = 2,
    @ApiModelProperty("The cluster ID to use for the index.")
    var clusterId: UUID? = null,

    @JsonIgnore
    @ApiModelProperty("The Project Id to create the route spec under.")
    var projectId: UUID? = null
)

/**
 * An IndexMappingVersion is a version of an ES mapping found on disk
 * or packaged with the Archivist that can be used to make an [IndexRoute]
 *
 * @property mapping The name of the mapping.
 * @property mappingMajorVer The major version of the mapping.
 */
@ApiModel(
    "IndexMappingVersion",
    description = "An IndexMappingVersion is a version of an ES mapping found on disk or packaged with the Archivist that can be used to make an [IndexRoute]"
)
class IndexMappingVersion(
    @ApiModelProperty("The name of the mapping.")
    val mapping: String,
    @ApiModelProperty("The major version of the mapping.")
    val mappingMajorVer: Int
)

/**
 * The ESClientCacheKey is used to lookup or create cached ElasticSearch client
 * instances.
 *
 * @property clusterUrl The url to the cluster
 * @property indexName The name of the index.
 * @property indexUrl The full URL to the index.
 */
@ApiModel(
    "EsClientCacheKey",
    description = "The ESClientCacheKey is used to lookup or create cached ElasticSearch client instances."
)
class EsClientCacheKey(
    @ApiModelProperty(" The url to the cluster")
    val clusterUrl: String,
    @ApiModelProperty("The name of the index.")
    val indexName: String,
    val majorVersion: Int
) {
    @ApiModelProperty("The full URL to the index.")
    val indexUrl = "$clusterUrl/$indexName"
}

/**
 * A class for filtering [IndexRoute]s
 */
class IndexRouteFilter(
    val ids: List<UUID>? = null,
    val clusterIds: List<UUID>? = null,
    val mappings: List<String>? = null,
    val projectIds: List<UUID>? = null
) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "id" to "index_route.pk_index_route",
            "projectId" to "index_route.pk_project",
            "clusterUrl" to "index_cluster.str_url",
            "mapping" to "index_route.str_mapping_type",
            "timeCreated" to "index_route.time_created"
        )

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("timeCreated:desc")
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("index_route.pk_index_route", it.size))
            addToValues(it)
        }

        clusterIds?.let {
            addToWhere(JdbcUtils.inClause("index_route.pk_index_cluster", it.size))
            addToValues(it)
        }

        projectIds?.let {
            addToWhere(JdbcUtils.inClause("index_route.pk_project", it.size))
            addToValues(it)
        }

        mappings?.let {
            addToWhere(JdbcUtils.inClause("index_route.str_mapping_type", it.size))
            addToValues(it)
        }
    }
}
