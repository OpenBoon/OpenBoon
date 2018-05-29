package com.zorroa.archivist.domain

import com.zorroa.sdk.domain.Asset
import com.zorroa.sdk.domain.Document
import org.elasticsearch.action.search.ClearScrollRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest

import org.elasticsearch.client.RestHighLevelClient
import java.util.*
import java.util.function.Consumer

class ScanAndScrollAssetIterator(private val client: RestHighLevelClient,
                                 private val rsp: SearchResponse,
                                 private var maxResults: Long) : Iterable<Document> {

    override fun iterator(): Iterator<Document> {
        return object : Iterator<Document> {

            internal var hits = rsp.hits.hits
            private var index = 0
            private var count = 0

            init {
                if (maxResults == 0L) {
                    maxResults = rsp.hits.totalHits
                }
            }

            override fun hasNext(): Boolean {
                if (index >= hits.size) {
                    val sr = SearchScrollRequest()
                    sr.scrollId(rsp.scrollId)
                    sr.scroll("1m")
                    hits = client.searchScroll(sr).hits.hits
                    index = 0
                }

                val hasMore = index < hits.size && count < maxResults
                if (!hasMore) {
                    var csr = ClearScrollRequest()
                    csr.addScrollId(rsp.scrollId)
                    client.clearScroll(csr)
                }
                return hasMore
            }

            override fun next(): Asset {
                val asset = Asset()
                val hit = hits[index++]

                asset.document = hit.sourceAsMap
                asset.id = hit.id

                count++
                return asset
            }

            override fun forEachRemaining(action: Consumer<in Document>) {
                throw UnsupportedOperationException()
            }
        }
    }

    override fun forEach(action: Consumer<in Document>) {
        throw UnsupportedOperationException()
    }

    override fun spliterator(): Spliterator<Document> {
        throw UnsupportedOperationException()
    }
}
