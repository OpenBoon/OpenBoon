package com.zorroa.archivist.elastic

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.DeprecationHandler
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.SimpleQueryStringBuilder
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder

/**
 * ElasticSearch utility functions.
 */
object ElasticUtils {

    val searchModule = SearchModule(Settings.EMPTY, false, emptyList())
    val xContentRegistry = NamedXContentRegistry(searchModule.namedXContents)

    /**
     * Parse the given ES query string and return a QueryBuilder.
     */
    fun parse(query: String): QueryBuilder {
        val parser = XContentFactory.xContent(XContentType.JSON).createParser(
            xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, query
        )

        val ssb = SearchSourceBuilder.fromXContent(parser)
        return ssb.query()
    }
}