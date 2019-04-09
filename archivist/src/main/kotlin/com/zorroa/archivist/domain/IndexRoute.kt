package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*


/**
 * An IndexRoute points to a unqiue ES cluster and index name.
 *
 * @property id The unique ID of the index route.
 * @property clusterUrl The URL to the ES cluster.
 * @property indexName The name of the ES index.
 * @property mappingType The mapping type. This is extracted from the
 * mapping file name, not the ES type.
 * @property mappingMajorVer The major version of the mapping file.
 * @property mappingMinorVer The minor version of the mapping file in a date format.
 * @property closed True if the index is closed and not in use.
 * @property replicas Number of index replicas.
 * @property shards Number of shards.
 * @property defaultPool True if the index route is in the default Pool.
 * @property indexUrl The ES index URL, or the cluster URL and index name combined.
 * @property useRouteKey Store all of an Organizations data into a single shard.
 */
class IndexRoute(
        val id: UUID,
        val clusterUrl: String,
        val indexName: String,
        val mappingType: String,
        val mappingMajorVer: Int,
        val mappingMinorVer: Int,
        val closed: Boolean,
        val replicas: Int,
        val shards: Int,
        val defaultPool: Boolean,
        val useRouteKey: Boolean)
{

    var orgCount: Int = 0
    val indexUrl = "$clusterUrl/$indexName"

    /**
     * Return an [EsClientCacheKey] which will apply writes across all shards.
     */
    @JsonIgnore
    fun esClientCacheKey() : EsClientCacheKey {
        return EsClientCacheKey(clusterUrl, indexName)
    }

    /**
     * Return an [EsClientCacheKey] for use with per-organization shards.
     */
    @JsonIgnore
    fun esClientCacheKey(rkey: String) : EsClientCacheKey {
        return EsClientCacheKey(clusterUrl, indexName, rkey)
    }
}

/**
 * The IndexRouteSpec defines all the values needed to create an index route.
 *
 * @property clusterUrl The URL of the ES cluster.
 * @property indexName The name of the ES index.
 * @property mappingType The type of mapping (not ES object type)
 * @property mappingMajorVer The major version to use. It will be patched up to highest level.
 * @property defaultPool Add the route to the default pool.
 * @property replicas The number of replicas there should be for each shard. Defaults to 2.
 * @property shards The number of shards in the index. Defaults to 5.
 */
class IndexRouteSpec(
        var clusterUrl: String,
        var indexName: String,
        var mappingType: String,
        var mappingMajorVer: Int,
        var defaultPool: Boolean,
        var replicas: Int=2,
        var shards: Int=5)


/**
 * The ESClientCacheKey is used to lookup or create cached ElasticSearch client
 * instances.
 *
 * @property clusterUrl The url to the cluster
 * @property indexName The name of the index.
 * @property routingKey A shard routing key string.
 * @property indexUrl The full URL to the index.
 */
class EsClientCacheKey(
        val clusterUrl: String,
        val indexName: String,
        val routingKey: String?=null) {

    val indexUrl = "$clusterUrl/$indexName"
}
