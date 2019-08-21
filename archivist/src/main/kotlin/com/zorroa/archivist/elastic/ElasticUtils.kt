package com.zorroa.archivist.elastic

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.DeprecationHandler
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder

/**
 * ElasticSearch utility functions.
 */
object ElasticQueryParser {

    val searchModule = SearchModule(Settings.EMPTY, false, emptyList())
    val xContentRegistry = NamedXContentRegistry(searchModule.namedXContents)

    fun parse(query: String): QueryBuilder {
        val parser = XContentFactory.xContent(XContentType.JSON).createParser(
            xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, query
        )

        val ssb = SearchSourceBuilder.fromXContent(parser)
        return ssb.query()
    }
}