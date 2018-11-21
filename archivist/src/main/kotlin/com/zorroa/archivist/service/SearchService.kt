package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.*
import com.zorroa.archivist.security.*
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.common.clients.EsClientCache
import com.zorroa.common.clients.SearchBuilder
import com.zorroa.common.util.Json
import org.elasticsearch.action.search.ClearScrollRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.common.geo.GeoPoint
import org.elasticsearch.common.lucene.search.function.CombineFunction
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.common.xcontent.ToXContent
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.geoBoundingBoxQuery
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.FieldSortBuilder.DOC_FIELD_NAME
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search.suggest.Suggest
import org.elasticsearch.search.suggest.SuggestBuilder
import org.elasticsearch.search.suggest.SuggestBuilders
import org.elasticsearch.search.suggest.completion.CompletionSuggestion
import org.elasticsearch.search.suggest.completion.context.CategoryQueryContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import java.io.OutputStream
import java.util.*
import java.util.stream.Collectors

interface SearchService {

    fun count(builder: AssetSearch): Long

    fun count(ids: List<UUID>, search: AssetSearch?): List<Long>

    fun count(folder: Folder): Long

    fun getSuggestTerms(text: String): List<String>

    fun scanAndScroll(search: AssetSearch, maxResults: Long, clamp:Boolean=false): Iterable<Document>

    /**
     * Execute a scan and scroll and for every hit, call the given function.
     * @param search An asset search
     * @param fetchSource Set to true if your function requires the full doc
     * @param func the function to call for each batch
     */
    fun scanAndScroll(search: AssetSearch, fetchSource: Boolean, func: (hits: SearchHits)-> Unit)

    /**
     * Execute the AssetSearch with the given Paging object.
     *
     * @param page
     * @param search
     * @return
     */
    fun search(page: Pager, search: AssetSearch): PagedList<Document>

    fun search(search: AssetSearch): SearchResponse

    @Throws(IOException::class)
    fun search(page: Pager, search: AssetSearch, stream: OutputStream)

    /**
     * Return the next page of an asset scroll.
     *
     * @param id
     * @param timeout
     * @return
     */
    fun scroll(id: String, timeout: String): PagedList<Document>

    fun buildSearch(search: AssetSearch, type: String): SearchBuilder

    fun getQuery(search: AssetSearch): QueryBuilder
}

class SearchContext(val linkedFolders: MutableSet<UUID>,
                    val perms: Boolean,
                    val postFilter: Boolean,
                    var score: Boolean=false)

@Service
class SearchServiceImpl @Autowired constructor(
        val indexDao: IndexDao,
        val esClientCache: EsClientCache,
        val properties: ApplicationProperties

): SearchService {
    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var logService: EventLogService

    @Autowired
    internal lateinit var fieldService: FieldService

    override fun count(builder: AssetSearch): Long {
        val rest = esClientCache[getOrgId()]
        return rest.client.search(buildSearch(builder, "asset").request).hits.totalHits
    }

    override fun count(ids: List<UUID>, search: AssetSearch?): List<Long> {
        val counts = Lists.newArrayListWithCapacity<Long>(ids.size)
        if (search != null) {
            // Replace any existing folders with each folder to get count.
            // FIXME: Use aggregation for simple folders.
            var filter: AssetFilter? = search.filter
            if (filter == null) {
                filter = AssetFilter()
            }
            var links: MutableMap<String, List<Any>>? = filter.links
            if (links == null) {
                links = Maps.newHashMap()
            }
            for (id in ids) {
                links!!.put("folder", Arrays.asList<Any>(id))
                filter.links = links
                search.filter = filter
                val count = count(search)
                counts.add(count)
            }
        } else {
            for (id in ids) {
                try {
                    val count = count(folderService.get(id))
                    counts.add(count)
                } catch (ignore: Exception) {
                    // probably don't have access to the folder.
                    counts.add(0L)
                }

            }
        }

        return counts
    }

    override fun count(folder: Folder): Long {
        var search: AssetSearch? = folder.search
        return if (search == null) {
            search = AssetSearch()
            search.addToFilter().addToLinks("folder", folder.id)
            count(search)
        } else {
            count(search)
        }
    }

    override fun getSuggestTerms(text: String): List<String> {
        val rest = esClientCache[getOrgId()]
        val builder = SearchSourceBuilder()
        val suggestBuilder = SuggestBuilder()
        val req = rest.newSearchRequest()
        req.source(builder)
        builder.suggest(suggestBuilder)

        val ctx = mapOf("organization" to
                listOf<ToXContent>(CategoryQueryContext.builder()
                        .setCategory(getUser().organizationId.toString())
                        .setBoost(1)
                        .setPrefix(false).build()))

        val fields = fieldService.getFields("asset")["keywords"] ?: return mutableListOf()

        for ((idx, field) in fields.withIndex()) {
            val completion = SuggestBuilders.completionSuggestion("$field.suggest")
                    .text(text).contexts(ctx)
            suggestBuilder.addSuggestion("suggest$idx", completion)
        }

        val unique = Sets.newTreeSet<String>()
        val suggest = rest.client.search(req).suggest

        for ((idx, _) in fields.withIndex()) {
            val comp : CompletionSuggestion = suggest.getSuggestion("suggest$idx") ?: continue
            for (e in comp) {
                for (o in e.options) {
                    val opt = o as Suggest.Suggestion.Entry.Option
                    unique.add(opt.text.toString())
                }
            }
        }

        val result = unique.toList()
        result.sorted()
        return result
    }

    override fun scanAndScroll(search: AssetSearch, fetchSource: Boolean, func: (hits: SearchHits)-> Unit) {
        val rest = esClientCache[getOrgId()]
        val builder = rest.newSearchBuilder()
        builder.source.query(getQuery(search))
        builder.source.fetchSource(fetchSource)
        builder.source.size(100)
        builder.request.scroll(TimeValue(60000))

        var rsp = rest.client.search(builder.request)
        try {
            do {
                func(rsp.hits)
                val sr = SearchScrollRequest(rsp.scrollId)
                sr.scroll(TimeValue.timeValueSeconds(30))
                rsp = rest.client.searchScroll(sr)
            } while (rsp.hits.hits.isNotEmpty())
        } finally {
            try {
                val cs = ClearScrollRequest()
                cs.addScrollId(rsp.scrollId)
                rest.client.clearScroll(cs)
            } catch (e: IOException) {
                logger.warn("failed to clear scan/scroll request, ", e)
            }
        }
    }
    override fun scanAndScroll(search: AssetSearch, maxResults: Long, clamp:Boolean): Iterable<Document> {
        val rest = esClientCache[getOrgId()]
        val builder = rest.newSearchBuilder()
        builder.source.query(getQuery(search))
        builder.source.size(100)
        builder.request.scroll(TimeValue(60000))

        val rsp = rest.client.search(builder.request)

        if (!clamp && maxResults > 0 && rsp.hits.totalHits > maxResults) {
            throw IllegalArgumentException("Asset search has returned more than $maxResults results.")
        }

        return ScanAndScrollAssetIterator(rest.client, rsp, maxResults)
    }

    private fun isSearchLogged(page: Pager, search: AssetSearch): Boolean {
        if (!search.isEmpty && page.closestPage == 1) {
            val scroll = search.scroll
            if (scroll != null) {
                // Don't log subsequent searchs.
                if (scroll.id != null) {
                    return false
                }
            }
            return true
        }
        return false
    }

    override fun search(search: AssetSearch): SearchResponse {
        val rest = esClientCache[getOrgId()]
        return rest.client.search(buildSearch(search, "asset").request)
    }

    override fun search(page: Pager, search: AssetSearch): PagedList<Document> {
        /**
         * If the search is not empty (its a valid search) and the page
         * number is 1, then log the search.
         */
        if (isSearchLogged(page, search)) {
            logService.logAsync(UserLogSpec.build(LogAction.Search, search))
        }

        val rest = esClientCache[getOrgId()]
        if (search.scroll != null) {
            val scroll = search.scroll
            if (scroll.id != null) {
                val result = indexDao.getAll(scroll.id, scroll.timeout)
                if (result.size() == 0) {
                    val req = ClearScrollRequest()
                    req.addScrollId(scroll.id)
                    rest.client.clearScroll(req)
                }
                return result
            }
        }

        return indexDao.getAll(page, buildSearch(search, "asset"))
    }

    @Throws(IOException::class)
    override fun search(page: Pager, search: AssetSearch, stream: OutputStream) {
        /**
         * If the search is not empty (its a valid search) and the page
         * number is 1, then log the search.
         */
        if (isSearchLogged(page, search)) {
            logService.logAsync(UserLogSpec.build(LogAction.Search, search))
        }

        indexDao.getAll(page, buildSearch(search, "asset"), stream)
    }

    override fun scroll(id: String, timeout: String): PagedList<Document> {
        /**
         * Only log valid searches (the ones that are not for the whole repo)
         * since otherwise it creates a lot of logs of empty searches.
         */
        val rest = esClientCache[getOrgId()]
        val result = indexDao.getAll(id, timeout)
        if (result.size() == 0) {
            val req = ClearScrollRequest()
            req.addScrollId(id)
            rest.client.clearScroll(req)
        }
        return result
    }

    override fun buildSearch(search: AssetSearch, type: String): SearchBuilder {
        val rest = esClientCache[getOrgId()]

        val ssb = SearchSourceBuilder()
        ssb.query(getQuery(search))

        val req = rest.newSearchRequest()
        req.indices("archivist")
        req.types(type)
        req.searchType(SearchType.DFS_QUERY_THEN_FETCH)
        req.preference(getUserId().toString())
        req.source(ssb)

        if (search.aggs != null) {
            val result = mutableMapOf<String, Any>()
            for ((name, agg) in search.aggs) {
                if (agg.containsKey("filter")) {
                    /**
                     * ES no longer supports empty filter aggs, so if curator
                     * submits one, it gets replaced with a match_all query.
                     */
                    val filter = agg["filter"] as Map<String,Any>
                    if (filter.isEmpty()) {
                        agg["filter"] = mapOf<String,Map<String,Object>>("match_all" to mapOf())
                    }
                }

                result[name] = agg
            }
            val map = mutableMapOf("aggs" to search.aggs)
            val json = Json.serializeToString(map)

            val searchModule = SearchModule(Settings.EMPTY, false, Collections.emptyList())
            val parser = XContentFactory.xContent(XContentType.JSON).createParser(
                     NamedXContentRegistry(searchModule.namedXContents), json)

            val ssb2 = SearchSourceBuilder.fromXContent(parser)
            ssb2.aggregations().aggregatorFactories.forEach { ssb.aggregation(it) }
        }

        if (search.postFilter != null) {
            val query = QueryBuilders.boolQuery()
            applyFilterToQuery(search.postFilter, query, mutableSetOf())
            ssb.postFilter(query)
        }

        if (search.scroll != null) {
            req.searchType(SearchType.QUERY_THEN_FETCH)
            if (search.scroll.timeout != null) {
                req.scroll(search.scroll.timeout)
            }
        }

        if (search.fields != null) {
            ssb.fetchSource(search.fields, arrayOf("media.content"))
        }

        if (search.scroll != null) {
            ssb.sort(DOC_FIELD_NAME, SortOrder.ASC)
        } else {
            if (search.order != null) {
                val fields = fieldService.getFields("asset")
                for (searchOrder in search.order) {
                    val sortOrder = if (searchOrder.ascending) SortOrder.ASC else SortOrder.DESC
                    // Make sure to use .raw for strings
                    if (!searchOrder.field.endsWith(".raw") &&
                            fields.getValue("string").contains(searchOrder.field)) {
                        searchOrder.field = searchOrder.field + ".raw"
                    }
                    ssb.sort(searchOrder.field, sortOrder)
                }
            } else {
                ssb.sort("_score", SortOrder.DESC)
                getDefaultSort().forEach { t, u ->
                    ssb.sort(t, u)
                }
            }
        }

        return rest.newSearchBuilder(req, ssb)
    }

    private fun getAggregations(search: AssetSearch): Map<String, Any>? {
        if (search.aggs == null) {
            return null
        }
        val result = mutableMapOf<String, Any>()
        for ((key, value) in search.aggs) {
            result[key] = value
        }

        return result
    }

    override fun getQuery(search: AssetSearch): QueryBuilder {
        return getQuery(search, Sets.newHashSet(), true, false)
    }

    private fun getQuery(search: AssetSearch, linkedFolders: MutableSet<UUID>, perms: Boolean, postFilter: Boolean): QueryBuilder {
        val query = QueryBuilders.boolQuery()
        query.filter(getOrganizationFilter())

        if (perms) {
            val permsQuery = getPermissionsFilter(search.access)
            if (permsQuery != null) {
                query.filter(permsQuery)
            }
        }

        if (search == null || (search.filter == null && search.query == null)) {
            query.must(QueryBuilders.matchAllQuery())
            return query
        }

        query.minimumShouldMatch(1)
        val assetBool = QueryBuilders.boolQuery()
        if (search.isQuerySet) {
            assetBool.must(getQueryStringQuery(search))
        }

        var filter: AssetFilter? = search.filter
        if (filter != null) {
            applyFilterToQuery(filter, assetBool, linkedFolders)
        }

        // Folders apply their post filter, but the main search// applies the post filter in the SearchRequest.
        // Aggs will be limited to the folders (correct), but
        // not to the filters in the top-level search.
        if (postFilter) {
            filter = search.postFilter
            if (filter != null) {
                applyFilterToQuery(filter, assetBool, linkedFolders)
            }
        }

        if (assetBool.hasClauses()) {
            query.should(assetBool)
        }

        if (properties.getBoolean("archivist.debug-mode.enabled")) {
            XContentFactory.jsonBuilder().use { builder->
                query.toXContent(builder, ToXContent.EMPTY_PARAMS)
                logger.info("SEARCH: {}", builder.string())
            }
        }
        return query
    }

    private fun linkQuery(query: BoolQueryBuilder, filter: AssetFilter, linkedFolders: MutableSet<UUID>) {

        val staticBool = QueryBuilders.boolQuery()

        val links = filter.links
        for ((key, value) in links) {
            if (key == "folder") {
                continue
            }
            staticBool.should(QueryBuilders.termsQuery("system.links.$key", value))
        }

        /*
         * Now do the recursive bit on just folders.
         */
        if (links.containsKey("folder")) {

            val folders = links["folder"]!!
                    .stream()
                    .map { f -> UUID.fromString(f.toString()) }
                    .filter { f -> !linkedFolders.contains(f) }
                    .collect(Collectors.toSet())

            val recursive = if (filter.recursive == null) true else filter.recursive

            if (recursive) {
                val childFolders = Sets.newHashSetWithExpectedSize<UUID>(32)

                for (folder in folderService.getAllDescendants(
                        folderService.getAll(folders), true, true)) {

                    if (linkedFolders.contains(folder.id)) {
                        continue
                    }
                    linkedFolders.add(folder.id)

                    /**
                     * Not going to allow people to add assets manually
                     * to smart folders, unless its to the smart query itself.
                     */
                    if (folder.search != null) {
                        folder.search.aggs = null
                        staticBool.should(getQuery(folder.search, linkedFolders, false, true))
                    }

                    /**
                     * We don't allow dyhi folders to have manual entries.
                     */
                    if (folder.dyhiId == null && !folder.dyhiRoot) {
                        childFolders.add(folder.id)
                        if (childFolders.size >= 1024) {
                            break
                        }
                    }
                }

                if (!childFolders.isEmpty()) {
                    staticBool.should(QueryBuilders.termsQuery("system.links.folder", childFolders))
                }
            } else {
                staticBool.should(QueryBuilders.termsQuery("system.links.folder", folders))
            }
        }

        query.must(staticBool)
    }

    private fun getQueryStringQuery(search: AssetSearch): QueryBuilder {
        val queryFields = if (JdbcUtils.isValid(search.queryFields)) {
            search.queryFields
        } else {
            fieldService.getQueryFields()
        }

        val qstring = QueryBuilders.queryStringQuery(search.query)
        qstring.allowLeadingWildcard(false)
        qstring.lenient(true) // ignores qstring errors
        for ((key, value) in queryFields) {
            qstring.field(key, value)
        }
        return qstring

    }

    /**
     * Apply the given filter to the overall boolean query.
     *
     * @param filter
     * @param query;
     * @return
     */
    private fun applyFilterToQuery(filter: AssetFilter, query: BoolQueryBuilder, linkedFolders: MutableSet<UUID>) {

        if (filter.links != null) {
            linkQuery(query, filter, linkedFolders)
        }

        if (filter.prefix != null) {

            val prefixMust = QueryBuilders.boolQuery()
            // models elasticsearch, the Map<String,Object> allows for a boost property
            for ((key, value) in filter.prefix) {
                val prefixFilter = QueryBuilders.prefixQuery(key,
                        value["prefix"] as String).boost(
                        ((value as java.util.Map<String, Any>).getOrDefault(
                                "boost", 1.0) as Double).toFloat())

                prefixMust.should(prefixFilter)
            }
            query.must(prefixMust)
        }

        if (filter.exists != null) {
            for (term in filter.exists) {
                val existsFilter = QueryBuilders.existsQuery(term)
                query.must(existsFilter)
            }
        }

        if (filter.missing != null) {
            for (term in filter.missing) {
                val existsFilter = QueryBuilders.existsQuery(term)
                query.mustNot(existsFilter)
            }
        }

        if (filter.terms != null) {
            for ((key, value) in filter.terms) {
                val values = value.orEmpty().filterNotNull()
                if (values.isNotEmpty()) {
                    val termsQuery = QueryBuilders.termsQuery(fieldService.dotRaw(key), value)
                    query.must(termsQuery)
                }
            }
        }

        if (filter.range != null) {
            for ((field, rq) in filter.range) {
                val rqb = RangeQueryBuilder(field)

                for (f in RangeQuery::class.java.declaredFields) {
                    try {
                        val m = RangeQueryBuilder::class.java.getMethod(f.name, f.type)
                        val v = f.get(rq) ?: continue
                        m.invoke(rqb, v)
                    } catch (e: Exception) {
                        throw IllegalArgumentException(
                                "RangeQueryBuilder has no '" + f.name + "' method")
                    }

                }
                query.must(rqb)
            }
        }

        if (filter.scripts != null) {

            for (script in filter.scripts) {
                val scriptFilterBuilder = QueryBuilders.scriptQuery(Script(ScriptType.INLINE,
                        "painless", script.script, script.params))
                query.must(scriptFilterBuilder)
            }
        }

        if (filter.similarity != null) {
            handleHammingFilter(filter.similarity, query)
        }

        if (filter.kwconf != null) {
            handleKwConfFilter(filter.kwconf, query)
        }


        // Recursively add bool sub-filters for must, must_not and should
        if (filter.must != null) {
            for (f in filter.must) {
                val must = QueryBuilders.boolQuery()
                this.applyFilterToQuery(f, must, linkedFolders)
                query.must(must)
            }
        }

        if (filter.mustNot != null) {
            for (f in filter.mustNot) {
                val mustNot = QueryBuilders.boolQuery()
                this.applyFilterToQuery(f, mustNot, linkedFolders)
                query.mustNot(mustNot)
            }
        }

        if (filter.should != null) {
            for (f in filter.should) {
                val should = QueryBuilders.boolQuery()
                this.applyFilterToQuery(f, should, linkedFolders)
                query.should(should)
            }
        }

        if (filter.geo_bounding_box != null) {
            val bbox = filter.geo_bounding_box
            for ((field, value) in bbox) {
                if (value != null) {
                    val tl = value.topLeftPoint()
                    val br = value.bottomRightPoint()
                    val bboxQuery = geoBoundingBoxQuery(field)
                            .setCorners(tl[0], tl[1], br[0], br[1])
                    query.should(bboxQuery)
                }
            }
        }
    }

    private fun handleKwConfFilter(filters: Map<String, KwConfFilter>,  query: BoolQueryBuilder) {
        val bool = QueryBuilders.boolQuery()
        query.must(bool)

        for ((field, filter) in filters) {
            val args = mutableMapOf<String, Any>()
            args["field"] = field
            args["keywords"] = filter.keywords
            args["range"] = filter.range

            val fsqb = QueryBuilders.functionScoreQuery(
                    ScoreFunctionBuilders.scriptFunction(Script(ScriptType.INLINE,
                            "zorroa-kwconf", "kwconf", args)))

            fsqb.minScore = filter.range[0].toFloat()
            fsqb.boostMode(CombineFunction.REPLACE)
            fsqb.scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY)

            val fbool =  QueryBuilders.boolQuery()
            fbool.must(fsqb)
            fbool.must(QueryBuilders.termsQuery("$field.keyword.raw", filter.keywords))
            bool.should(fbool)
        }
    }

    private fun handleHammingFilter(filters: Map<String, SimilarityFilter>, query: BoolQueryBuilder) {

        val hammingBool = QueryBuilders.boolQuery()
        query.must(hammingBool)

        for ((field, filter) in filters) {

            /**
             * Resolve any asset Ids in the hash list.
             */

            val hashes = mutableListOf<String>()
            val weights = mutableListOf<Float>()

            for (hash in filter.hashes) {
                var hashValue: String? = hash.hash
                if (JdbcUtils.isUUID(hashValue)) {
                    hashValue = indexDao.getFieldValue(hashValue as String, field)
                }

                if (hashValue != null) {
                    hashes.add(hashValue)
                    weights.add(if (hash.weight == null) 1.0f else hash.weight)
                }
                else {
                    logger.warn("could not find value at: {} {}", hashValue, field)
                }
            }

            val args = mutableMapOf<String, Any>()
            args["field"] = field
            args["hashes"] = hashes.joinToString(",")
            args["weights"] = weights.joinToString(",")
            args["minScore"] = filter.minScore

            val fsqb = QueryBuilders.functionScoreQuery(
                    ScoreFunctionBuilders.scriptFunction(Script(ScriptType.INLINE,
                            "zorroa-similarity", "similarity", args)))

            fsqb.minScore = filter.minScore / 100.0f
            fsqb.boostMode(CombineFunction.REPLACE)
            fsqb.scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY)

            hammingBool.should(fsqb)

        }
    }

    fun getDefaultSort() : Map<String, SortOrder> {
        val result = linkedMapOf<String, SortOrder>()
        val sortFields = properties.getList("archivist.search.sortFields")

        sortFields.asSequence()
                .map { it.split(":", limit = 2) }
                .forEach {
                    val order = try {
                        SortOrder.valueOf(it[1])
                    } catch(e:IllegalArgumentException) {
                        SortOrder.ASC
                    }
                    result[it[0]] = order
                }

        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SearchServiceImpl::class.java)
    }
}
