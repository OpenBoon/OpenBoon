package com.zorroa.archivist.domain

import com.fasterxml.jackson.core.type.TypeReference
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Asset Search", description = "Stores an Asset search, is modeled after an ElasticSearch request.")
class AssetSearch(

    @ApiModelProperty(
        "The search to execute",
        reference = "https://www.elastic.co/guide/en/elasticsearch/reference/7.5/query-filter-context.html"
    )
    val search: Map<String, Any>? = null
)

/**
 * A SimilarityFilter describes a hash and a weight.
 */
class SimilarityFilter(
    val hash: String,
    val weight: Float = 1.0f
) {
    companion object {

        val JSON_MAP_OF: TypeReference<Map<String, List<SimilarityFilter>>> =
            object : TypeReference<Map<String, List<SimilarityFilter>>>() {}
    }
}
