package com.zorroa.archivist.domain

import com.zorroa.sdk.search.AssetSearch

data class OnlineFileCheckReq(
        val search: AssetSearch
)

data class OnlineFileCheckRsp(
        var total : Int = 0,
        var totalOnline : Int = 0,
        var totalOffline : Int = 0,
        val offlineAssetIds : MutableList<String> = mutableListOf()
)

data class LfsRequest (
        val path: String?,
        val prefix: String?,
        val types: Set<String> = setOf()
)

