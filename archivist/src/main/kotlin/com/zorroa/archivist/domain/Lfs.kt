package com.zorroa.archivist.domain

import com.zorroa.sdk.search.AssetSearch
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.LongAdder

data class OnlineFileCheckRequest(
        val search: AssetSearch
)

data class OnlineFileCheckResponse(
        var total : LongAdder = LongAdder(),
        var totalOnline : LongAdder = LongAdder(),
        var totalOffline : LongAdder = LongAdder(),
        val offlineAssetIds : Queue<String> = LinkedBlockingQueue()
)

data class LfsRequest (
        val path: String?,
        val prefix: String?,
        val types: Set<String> = setOf()
)

