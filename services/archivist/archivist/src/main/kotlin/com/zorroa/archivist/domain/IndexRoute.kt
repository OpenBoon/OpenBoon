package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.KDaoFilter
import com.zorroa.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * An IndexRoute points to a unqiue ES cluster and index name.
 *
 * @property id The unique ID of the index route.
 * @property clusterUrl The URL to the ES cluster.
 * @property indexName The name of the ES index.
 * @property mapping The mapping type. This is extracted from the
 * mapping file name, not the ES type.
 * @property mappingMajorVer The major version of the mapping file.
 * @property mappingMinorVer The minor version of the mapping file in a date format.
 * @property closed "True if the index is closed and not in use."
 * @property replicas Number of index replicas.
 * @property shards Number of shards.
 * @property defaultPool True if the index route is in the default Pool.
 * @property indexUrl The ES index URL, or the cluster URL and index name combined.
 */
@ApiModel("IndexRoute", description = "TaskErrorEvents are emitted by the processing system if an an exception is thrown while processing")

class IndexRoute(
        @ApiModelProperty("The unique ID of the index route.")
        val id: UUID,
        val projectId: UUID,
        @ApiModelProperty("The URL to the ES cluster.")
        val clusterUrl: String,
        @ApiModelProperty("The name of the ES index.")
        val indexName: String,
        @ApiModelProperty("The mapping type. This is extracted from the mapping file name, not the ES type.")
        val mapping: String,
        @ApiModelProperty("The major version of the mapping file.")
        val mappingMajorVer: Int,
        @ApiModelProperty("The minor version of the mapping file in a date format.")
        val mappingMinorVer: Int,
        @ApiModelProperty("True if the index is closed and not in use.")
        val closed: Boolean,
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
        return EsClientCacheKey(clusterUrl, indexName)
    }
}

/**
 * The IndexRouteSpec defines all the values needed to create an index route.
 *
 * @property clusterUrl The URL of the ES cluster.
 * @property indexName The name of the ES index.
 * @property mapping The type of mapping (not ES object type)
 * @property mappingMajorVer The major version to use. It will be patched up to highest level.
 * @property defaultPool Add the route to the default pool.
 * @property replicas The number of replicas there should be for each shard. Defaults to 2.
 * @property shards The number of shards in the index. Defaults to 5.
 */
@ApiModel("IndexRouteSpec", description = "The IndexRouteSpec defines all the values needed to create an index route.")
class IndexRouteSpec(
        @ApiModelProperty("The URL of the ES cluster.")
        var clusterUrl: String,
        @ApiModelProperty("The name of the ES index.")
        var indexName: String,
        @ApiModelProperty("The type of mapping (not ES object type)")
        var mapping: String,
        @ApiModelProperty("The major version to use. It will be patched up to highest level.")
        var mappingMajorVer: Int,
        @ApiModelProperty("The number of replicas there should be for each shard. Defaults to 2.")
        var replicas: Int = 1,
        @ApiModelProperty(" The number of shards in the index. Defaults to 5.")
        var shards: Int = 5
)

/**
 * An IndexMappingVersion is a version of an ES mapping found on disk
 * or packaged with the Archivist that can be used to make an [IndexRoute]
 *
 * @property mapping The name of the mapping.
 * @property mappingMajorVer The major version of the mapping.
 */
@ApiModel("IndexMappingVersion", description = "An IndexMappingVersion is a version of an ES mapping found on disk or packaged with the Archivist that can be used to make an [IndexRoute]")
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
@ApiModel("EsClientCacheKey", description = "The ESClientCacheKey is used to lookup or create cached ElasticSearch client instances.")
class EsClientCacheKey(
        @ApiModelProperty(" The url to the cluster")
        val clusterUrl: String,
        @ApiModelProperty("The name of the index.")
        val indexName: String
) {
    @ApiModelProperty("The full URL to the index.")
    val indexUrl = "$clusterUrl/$indexName"
}

/**
 * A class for filtering [IndexRoute]s
 */
class IndexRouteFilter(
    val ids: List<UUID>? = null,
    val clusterUrls: List<String>? = null,
    val mappings: List<String>? = null
) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "id" to "index_route.pk_index_route",
            "clusterUrl" to "index_route.str_url",
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

        clusterUrls?.let {
            addToWhere(JdbcUtils.inClause("index_route.str_url", it.size))
            addToValues(it)
        }

        mappings?.let {
            addToWhere(JdbcUtils.inClause("index_route.str_mapping_type", it.size))
            addToValues(it)
        }
    }
}
