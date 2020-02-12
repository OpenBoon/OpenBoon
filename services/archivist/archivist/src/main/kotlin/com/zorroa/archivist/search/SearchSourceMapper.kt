package com.zorroa.archivist.search

import com.zorroa.zmlp.util.Json
import org.elasticsearch.common.Strings
import org.elasticsearch.common.lucene.search.function.CombineFunction
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.DeprecationHandler
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object SearchSourceMapper {

    val logger: Logger = LoggerFactory.getLogger(SearchSourceMapper::class.java)

    private val searchModule = SearchModule(Settings.EMPTY, false, emptyList())

    private val xContentRegistry = NamedXContentRegistry(searchModule.namedXContents)

    private val allowedSearchProperties = setOf(
        "query", "from", "size", "timeout",
        "post_filter", "minscore", "suggest",
        "highlight", "collapse",
        "slice", "aggs", "aggregations", "sort"
    )

    fun convert(search: Map<String, Any>): SearchSourceBuilder {

        // Filters out search options that are not supported.
        val searchSource = search.filterKeys { it in allowedSearchProperties }
        val parser = XContentFactory.xContent(XContentType.JSON).createParser(
            xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            Json.serializeToString(searchSource)
        )

        val ssb = SearchSourceBuilder.fromXContent(parser)
        if (ssb.query() == null) {
            ssb.query(QueryBuilders.matchAllQuery())
        }

        if (logger.isDebugEnabled) {
            logger.debug("SEARCH : {}", Strings.toString(ssb, true, true))
        }

        return ssb
    }

}
