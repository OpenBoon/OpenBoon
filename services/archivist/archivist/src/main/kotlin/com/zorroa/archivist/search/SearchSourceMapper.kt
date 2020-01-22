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
            Json.serializeToString(parseZmlpPlugins(searchSource))
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

    fun parseZmlpPlugins(searchSource: Map<String, Any>): Map<String, Any> {

        fun fix(map: Any): Any {
            if (map !is Map<*, *>) {
                return map
            }

            val curMap = map as Map<String, Any>
            return curMap.map { (k, v) ->
                when {
                    k == "similarity" -> {
                        val mapOfSimilarityHash = Json.Mapper.convertValue(v, SimilarityFilter.JSON_MAP_OF)
                        val minScore = map.getOrDefault("minScore", "75").toString().toInt()
                        "bool" to convertSimilaritySearch(mapOfSimilarityHash, minScore)
                    }
                    v is Map<*, *> -> {
                        k to fix(v)
                    }
                    else -> {
                        k to v
                    }
                }
            }.toMap()
        }

        return searchSource.map { (k, v) ->
            k to fix(v)
        }.toMap()
    }

    fun convertSimilaritySearch(filterMap: Map<String, List<SimilarityFilter>>, minScore: Int): Any {
        val hammingBool = QueryBuilders.boolQuery()
        val hashes = mutableListOf<String>()
        val weights = mutableListOf<Float>()

        for ((field, filters) in filterMap.entries) {
            for (filter in filters) {
                val hash = filter.hash
                hashes.add(hash)
                weights.add(filter.weight)
            }

            val args = mutableMapOf<String, Any>()
            args["field"] = field
            args["hashes"] = hashes.joinToString(",")
            args["weights"] = weights.joinToString(",")
            args["minScore"] = minScore

            val fsqb = QueryBuilders.functionScoreQuery(
                ScoreFunctionBuilders.scriptFunction(
                    Script(
                        ScriptType.INLINE,
                        "zorroa-similarity", "similarity", args
                    )
                )
            )

            fsqb.minScore = minScore / 100.0f
            fsqb.boostMode(CombineFunction.REPLACE)
            fsqb.scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY)

            hammingBool.should(fsqb)
        }
        val dsl = Strings.toString(hammingBool, true, true)
        return Json.Mapper.readValue(dsl, Json.GENERIC_MAP).getValue("bool")
    }
}