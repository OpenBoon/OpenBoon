package com.zorroa.archivist.domain

import com.zorroa.sdk.search.AssetSearch

data class LfsExistsSearch(
        val search: AssetSearch
)

data class LfsRequest (
        val path: String?,
        val prefix: String?,
        val types: Set<String> = setOf()
)

