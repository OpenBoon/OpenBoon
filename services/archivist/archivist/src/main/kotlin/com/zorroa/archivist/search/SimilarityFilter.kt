package com.zorroa.archivist.search

import com.fasterxml.jackson.core.type.TypeReference

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
