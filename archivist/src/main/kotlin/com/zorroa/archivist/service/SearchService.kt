package com.zorroa.archivist.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.*
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.AssetDao
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.archivist.security.getPermissionsFilter
import com.zorroa.archivist.security.getUsername
import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.search.AssetFilter
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.search.RangeQuery
import com.zorroa.sdk.search.SimilarityFilter
import com.zorroa.sdk.util.Json
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.action.suggest.SuggestResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.index.query.support.QueryInnerHitBuilder
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptService
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search.sort.SortParseElement
import org.elasticsearch.search.suggest.Suggest
import org.elasticsearch.search.suggest.completion.CompletionSuggestion
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

interface SearchService {

    fun search(builder: AssetSearch): SearchResponse
    fun count(builder: AssetSearch): Long

    fun count(ids: List<UUID>, search: AssetSearch?): List<Long>

    fun count(folder: Folder): Long

    fun suggest(text: String): SuggestResponse

    fun getSuggestTerms(text: String): List<String>

    fun scanAndScroll(search: AssetSearch, maxResults: Long, clamp:Boolean=false): Iterable<Document>

    /**
     * Execute the AssetSearch with the given Paging object.
     *
     * @param page
     * @param search
     * @return
     */
    fun search(page: Pager, search: AssetSearch): PagedList<Document>

    @Throws(IOException::class)
    fun search(page: Pager, search: AssetSearch, stream: OutputStream)

    @Throws(IOException::class)
    fun searchElements(page: Pager, search: AssetSearch, stream: OutputStream)

    /**
     * Return the next page of an asset scroll.
     *
     * @param id
     * @param timeout
     * @return
     */
    fun scroll(id: String, timeout: String): PagedList<Document>

    fun buildSearch(search: AssetSearch, type: String): SearchRequestBuilder

    fun getQuery(search: AssetSearch): QueryBuilder

    fun analyzeQuery(search: AssetSearch): List<String>
}

class SearchContext(val linkedFolders: MutableSet<UUID>,
                    val perms: Boolean,
                    val postFilter: Boolean,
                    var score: Boolean=false)

@Service
class SearchServiceImpl @Autowired constructor(
        val assetDao: AssetDao,
        val client: Client,
        val properties: ApplicationProperties

): SearchService {
    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var logService: EventLogService

    @Autowired
    internal lateinit var fieldService: FieldService

    @Value("\${zorroa.cluster.index.alias}")
    private lateinit var alias: String

    private val fieldMapCache = CacheBuilder.newBuilder()
            .maximumSize(2)
            .initialCapacity(3)
            .concurrencyLevel(1)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build(object : CacheLoader<String, Map<String, Set<String>>>() {
                @Throws(Exception::class)
                override fun load(key: String): Map<String, Set<String>> {
                    return fieldService.getFieldMap(key)
                }
            })

    override fun search(search: AssetSearch): SearchResponse {
        return buildSearch(search, "asset")
                .setFrom(if (search.from == null) 0 else search.from)
                .setSize(if (search.size == null) 10 else search.size).get()
    }

    override fun count(builder: AssetSearch): Long {
        return buildSearch(builder, "asset").setSize(0).get().hits.totalHits
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
        val builder = client.prepareSuggest(alias)
        val fields = fieldService.getFields("asset")["keywords"] ?: return mutableListOf()

        for (field in fields) {
            val completion = CompletionSuggestionBuilder(field)
                    .text(text)
                    .field("$field.suggest")
            builder.addSuggestion(completion)
        }

        val unique = Sets.newTreeSet<String>()
        val suggest = builder.get().suggest
        for (field in fields) {
            val comp = suggest.getSuggestion<CompletionSuggestion>(field) ?: continue
            for (e in comp.entries) {
                for (o in e.options) {
                    val opt = o as Suggest.Suggestion.Entry.Option
                    unique.add(opt.text.toString())
                }
            }
        }

        val result = Lists.newArrayList(unique)
        Collections.sort(result)
        return result
    }

    override fun suggest(text: String): SuggestResponse {
        val builder = client.prepareSuggest(alias)

        val completion = CompletionSuggestionBuilder("completions")
                .text(text)
                .field("keywords.suggest")
        builder.addSuggestion(completion)
        return builder.get()
    }

    override fun scanAndScroll(search: AssetSearch, maxResults: Long, clamp:Boolean): Iterable<Document> {
        val rsp = client.prepareSearch(alias)
                .setScroll(TimeValue(60000))
                .addSort("_doc", SortOrder.ASC)
                .setQuery(getQuery(search))
                .setSize(100).execute().actionGet()

        if (!clamp && maxResults > 0 && rsp.hits.totalHits > maxResults) {
            throw IllegalArgumentException("Asset search has returned more than $maxResults results.")
        }

        return ScanAndScrollAssetIterator(client, rsp, maxResults)
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

    override fun search(page: Pager, search: AssetSearch): PagedList<Document> {
        /**
         * If the search is not empty (its a valid search) and the page
         * number is 1, then log the search.
         */
        if (isSearchLogged(page, search)) {
            logService.logAsync(UserLogSpec.build(LogAction.Search, search))
        }

        if (search.scroll != null) {
            val scroll = search.scroll
            if (scroll.id != null) {
                val result = assetDao.getAll(scroll.id, scroll.timeout)
                if (result.size() == 0) {
                    client.prepareClearScroll().addScrollId(scroll.id)
                }
                return result
            }
        }

        return assetDao.getAll(page, buildSearch(search, "asset"))
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

        /**
         * If a query is set, analyze it.
         */
        val terms = analyzeQuery(search)

        assetDao.getAll(page, buildSearch(search, "asset"), stream, ImmutableMap.of("queryTerms", terms))
    }

    @Throws(IOException::class)
    override fun searchElements(page: Pager, search: AssetSearch, stream: OutputStream) {
        /**
         * If the search is not empty (its a valid search) and the page
         * number is 1, then log the search.
         */
        if (isSearchLogged(page, search)) {
            logService.logAsync(UserLogSpec.build(LogAction.Search, search))
        }

        assetDao.getAll(page, buildSearch(search, "element"), stream, ImmutableMap.of())
    }

    override fun scroll(id: String, timeout: String): PagedList<Document> {
        /**
         * Only log valid searches (the ones that are not for the whole repo)
         * since otherwise it creates a lot of logs of empty searches.
         */
        val result = assetDao.getAll(id, timeout)
        if (result.size() == 0) {
            client.prepareClearScroll().addScrollId(id)
        }
        return result
    }

    override fun buildSearch(search: AssetSearch, type: String): SearchRequestBuilder {
        val request = client.prepareSearch(alias)
                .setTypes(type)
                // This search type provides stable sorting but seems to ignore
                // the result si
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setPreference(getUsername())
                .setQuery(getQuery(search))

        if (search.aggs != null) {
            request.setAggregations(getAggregations(search))
        }

        if (search.postFilter != null) {
            val query = QueryBuilders.boolQuery()
            applyFilterToQuery(search.postFilter, query, mutableSetOf())
            request.setPostFilter(query)
        }

        if (search.scroll != null) {
            request.setSearchType(SearchType.QUERY_THEN_FETCH)
            if (search.scroll.timeout != null) {
                request.setScroll(search.scroll.timeout)
            }
        }

        if (search.fields != null) {
            request.setFetchSource(search.fields, arrayOf("content"))
        }

        if (search.scroll != null) {
            request.addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
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
                    request.addSort(searchOrder.field, sortOrder)
                }
            } else {
                request.addSort(SortParseElement.SCORE_FIELD_NAME, SortOrder.DESC)
                getDefaultSort().forEach { t, u ->
                    request.addSort(t, u)
                }
            }
        }

        if (properties.getBoolean("archivist.debug-mode.enabled")) {
            logger.info("SEARCH: {}", request)
        }
        return request
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

    private fun getQuery(search: AssetSearch?, linkedFolders: MutableSet<UUID>, perms: Boolean, postFilter: Boolean): QueryBuilder {

        val permsQuery = getPermissionsFilter()

        if (search == null) {
            return permsQuery ?: QueryBuilders.matchAllQuery()
        }

        val query = QueryBuilders.boolQuery()
        query.minimumNumberShouldMatch(1)

        val assetBool = QueryBuilders.boolQuery()

        if (perms && permsQuery != null) {
            query.filter(permsQuery)
        }

        if (search.isQuerySet) {
            assetBool.must(getQueryStringQuery(search))
        }

        var filter: AssetFilter? = search.filter
        if (filter != null) {
            applyFilterToQuery(filter, assetBool, linkedFolders)
        }

        val elementFilter = search.elementFilter
        if (elementFilter != null) {
            val elementBool = QueryBuilders.boolQuery()
            applyFilterToQuery(elementFilter, elementBool, mutableSetOf())
            query.should(QueryBuilders.hasChildQuery("element", elementBool)
                    .scoreMode("max")
                    .maxChildren(1)
                    .innerHit(QueryInnerHitBuilder().setSize(1)))
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

        return query
    }

    private fun linkQuery(query: BoolQueryBuilder, filter: AssetFilter, linkedFolders: MutableSet<UUID>) {

        val staticBool = QueryBuilders.boolQuery()

        val links = filter.links
        for ((key, value) in links) {
            if (key == "folder") {
                continue
            }
            staticBool.should(QueryBuilders.termsQuery("zorroa.links.$key", value))
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
                    if (folder.dyhiId == null && !folder.isDyhiRoot) {
                        childFolders.add(folder.id)
                        if (childFolders.size >= 1024) {
                            break
                        }
                    }
                }

                if (!childFolders.isEmpty()) {
                    staticBool.should(QueryBuilders.termsQuery("zorroa.links.folder", childFolders))
                }
            } else {
                staticBool.should(QueryBuilders.termsQuery("zorroa.links.folder", folders))
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
                val missingFilter = QueryBuilders.missingQuery(term)
                query.must(missingFilter)
            }
        }

        if (filter.terms != null) {
            for ((key, value) in filter.terms) {
                val termsQuery = QueryBuilders.termsQuery(fieldService.dotRaw(key), value)
                query.must(termsQuery)
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
                val scriptFilterBuilder = QueryBuilders.scriptQuery(Script(
                        script.script, ScriptService.ScriptType.INLINE, script.type, script.params))
                query.must(scriptFilterBuilder)
            }
        }

        // backwards compatible hamming.
        if (filter.hamming != null) {
            val hdf = Json.Mapper.convertValue<HammingDistanceFilter>(filter.hamming,
                    HammingDistanceFilter::class.java)

            val simFilter = SimilarityFilter()
            simFilter.hashes = mutableListOf()

            for ((index, value) in hdf.hashes.withIndex()) {
                val shash = SimilarityFilter.SimilarityHash()
                value?.let {
                    shash.hash = it.toString()
                    shash.order = index
                    shash.weight = hdf.weights.elementAtOrElse(index, { 1.0f })
                    simFilter.hashes.add(shash)
                }
            }

            handleHammingFilter(mapOf(hdf.field to simFilter), query)
        }
        else if (filter.similarity != null) {
            handleHammingFilter(filter.similarity, query)
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
    }

    private fun handleHammingFilter(filters: Map<String, SimilarityFilter>, query: BoolQueryBuilder) {

        val hammingBool = QueryBuilders.boolQuery()
        query.must(hammingBool)

        for ((field, filter) in filters) {

            /**
             * Resolve any asset Ids in the hash list.
             */

            val hashes = Lists.newArrayList<String>()
            val weights = Lists.newArrayList<Float>()

            for (hash in filter.hashes) {
                var hashValue: String = hash.hash
                logger.warn("hash value: {}", hashValue)
                if (JdbcUtils.isUUID(hashValue)) {
                    hashValue = assetDao.getFieldValue(hashValue, field)
                }

                if (hashValue != null) {
                    hashes.add(hashValue)
                    weights.add(if (hash.weight == null) 1.0f else hash.weight)
                }
                else {
                    logger.warn("could not find value at: {} {}", hashValue, field)
                }
            }

            val args = Maps.newHashMap<String, Any>()
            args["field"] = field
            args["hashes"] = hashes
            args["weights"] = weights
            args["minScore"] = filter.minScore

            val fsqb = QueryBuilders.functionScoreQuery(
                    ScoreFunctionBuilders.scriptFunction(Script(
                            "hammingDistance", ScriptService.ScriptType.INLINE, "native", args)))

            fsqb.setMinScore(filter.minScore / 100.0f)
            fsqb.boostMode("replace")
            fsqb.scoreMode("multiply")
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

    override fun analyzeQuery(search: AssetSearch): List<String> {
        return if (search.query == null || search.query.isEmpty()) {
            ImmutableList.of()
        } else client.admin().indices().prepareAnalyze(
                search.query)
                .setIndex("archivist")
                .get().tokens.stream().map { e -> e.term }.collect(Collectors.toList())

    }

    companion object {
        private val logger = LoggerFactory.getLogger(SearchServiceImpl::class.java)
    }
}
