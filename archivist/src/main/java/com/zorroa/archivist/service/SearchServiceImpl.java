package com.zorroa.archivist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.HideField;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.FieldDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.client.exception.ArchivistException;
import com.zorroa.sdk.client.exception.ArchivistReadException;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.search.*;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.support.QueryInnerHitBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by chambers on 9/25/15.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    AssetDao assetDao;

    @Autowired
    FolderService folderService;

    @Autowired
    EventLogService logService;

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    FieldDao fieldDao;

    private Map<String, SortOrder> defaultSortFields = Maps.newLinkedHashMap();

    @PostConstruct
    public void init() {
        initializeDefaultQuerySettings();
    }

    @Override
    public SearchResponse search(AssetSearch search) {
        SearchResponse rsp =  buildSearch(search, "asset")
                .setFrom(search.getFrom() == null ? 0 : search.getFrom())
                .setSize(search.getSize() == null ? 10 : search.getSize()).get();
        return rsp;
    }

    @Override
    public long count(AssetSearch builder) {
        return buildSearch(builder, "asset").setSize(0).get().getHits().getTotalHits();
    }

    @Override
    public List<Long> count(List<Integer> ids, AssetSearch search) {
        List<Long> counts = Lists.newArrayListWithCapacity(ids.size());
        if (search != null) {
            // Replace any existing folders with each folder to get count.
            // FIXME: Use aggregation for simple folders.
            AssetFilter filter = search.getFilter();
            if (filter == null) {
                filter = new AssetFilter();
            }
            Map<String, List<Object>> links = filter.getLinks();
            if (links == null) {
                links = Maps.newHashMap();
            }
            for (Integer id : ids) {
                links.put("folder", Arrays.asList(id));
                filter.setLinks(links);
                search.setFilter(filter);
                long count = count(search);
                counts.add(count);
            }
        } else {
            for (Integer id : ids) {
                try {
                    long count = count(folderService.get(id));
                    counts.add(count);
                } catch (Exception ignore) {
                    // probably don't have access to the folder.
                    counts.add(0L);
                }
            }
        }

        return counts;
    }

    @Override
    public long count(Folder folder) {
        AssetSearch search = folder.getSearch();
        if (search == null) {
            search = new AssetSearch();
            search.addToFilter().addToLinks("folder", folder.getId());
            return count(search);
        }
        else {
            return count(search);
        }
    }

    @Override
    public List<String> getSuggestTerms(String text) {
        SuggestRequestBuilder builder = client.prepareSuggest(alias);
        Set<String> fields = getFields("asset").get("completion");
        if (!JdbcUtils.isValid(fields)) {
            return Lists.newArrayList();
        }

        for (String field : fields) {
            CompletionSuggestionBuilder completion = new CompletionSuggestionBuilder(field)
                    .text(text)
                    .field(field);
            builder.addSuggestion(completion);
        }

        Set<String> unique = Sets.newTreeSet();
        Suggest suggest = builder.get().getSuggest();
        for (String field : fields) {
            CompletionSuggestion comp =  suggest.getSuggestion(field);
            if (comp == null) {
                continue;
            }
            for (Suggest.Suggestion.Entry e : comp.getEntries()) {
                for (Object o: e.getOptions()) {
                    Suggest.Suggestion.Entry.Option opt = (Suggest.Suggestion.Entry.Option) o;
                    unique.add(opt.getText().toString());
                }
            }
        }

        List<String> result = Lists.newArrayList(unique);
        Collections.sort(result);
        return result;
    }

    @Override
    public SuggestResponse suggest(String text) {
        SuggestRequestBuilder builder = client.prepareSuggest(alias);

        CompletionSuggestionBuilder completion = new CompletionSuggestionBuilder("completions")
                .text(text)
                .field("keywords.suggest");
        builder.addSuggestion(completion);
        return builder.get();
    }

    @Override
    public Iterable<Document> scanAndScroll(AssetSearch search, int maxResults) {
        SearchResponse rsp = client.prepareSearch(alias)
                .setScroll(new TimeValue(60000))
                .addSort("_doc", SortOrder.ASC)
                .setQuery(getQuery(search))
                .setSize(100).execute().actionGet();

        if (maxResults > 0 && rsp.getHits().getTotalHits() > maxResults) {
            throw new ArchivistReadException("Asset search has returned more than " + maxResults + " results.");
        }
        return new ScanAndScrollAssetIterator(client, rsp);
    }

    private boolean isSearchLogged(Pager page, AssetSearch search) {
        if (!search.isEmpty() && page.getClosestPage() == 1) {
            Scroll scroll = search.getScroll();
            if (scroll != null) {
                // Don't log subsequent searchs.
                if (scroll.getId() != null) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public PagedList<Document> search(Pager page, AssetSearch search) {
        /**
         * If the search is not empty (its a valid search) and the page
         * number is 1, then log the search.
         */
        if (isSearchLogged(page, search)) {
            logService.logAsync(new UserLogSpec().build(LogAction.Search, search));
        }

        if (search.getScroll() != null) {
            Scroll scroll = search.getScroll();
            if (scroll.getId() != null) {
                PagedList<Document> result = assetDao.getAll(scroll.getId(), scroll.getTimeout());
                if (result.size() == 0) {
                    client.prepareClearScroll().addScrollId(scroll.getId());
                }
                return result;
            }
        }

        return assetDao.getAll(page, buildSearch(search, "asset"));
    }

    @Override
    public void search(Pager page, AssetSearch search, OutputStream stream) throws IOException {
        /**
         * If the search is not empty (its a valid search) and the page
         * number is 1, then log the search.
         */
        if (isSearchLogged(page, search)) {
            logService.logAsync(new UserLogSpec().build(LogAction.Search, search));
        }

        /**
         * If a query is set, analyze it.
         */
        List<String> terms = analyzeQuery(search);

        assetDao.getAll(page, buildSearch(search, "asset"), stream, ImmutableMap.of("queryTerms", terms));
    }

    @Override
    public void searchElements(Pager page, AssetSearch search, OutputStream stream) throws IOException {
        /**
         * If the search is not empty (its a valid search) and the page
         * number is 1, then log the search.
         */
        if (isSearchLogged(page, search)) {
            logService.logAsync(new UserLogSpec().build(LogAction.Search, search));
        }

        assetDao.getAll(page, buildSearch(search, "element"), stream, ImmutableMap.of());
    }

    @Override
    public PagedList<Document> scroll(String id, String timeout) {
        /**
         * Only log valid searches (the ones that are not for the whole repo)
         * since otherwise it creates a lot of logs of empty searches.
         */
        PagedList<Document> result =  assetDao.getAll(id, timeout);
        if (result.size() == 0) {
            client.prepareClearScroll().addScrollId(id);
        }
        return result;
    }

    @Override
    public SearchRequestBuilder buildSearch(AssetSearch search, String type) {

        SearchRequestBuilder request = client.prepareSearch(alias)
                .setTypes(type)
                .setPreference(SecurityUtils.getUsername())
                .setQuery(getQuery(search));

        if (search.getAggs() != null) {
            request.setAggregations(getAggregations(search));
        }

        if (search.getPostFilter() != null) {
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            applyFilterToQuery(search.getPostFilter(), query, Sets.newHashSet());
            request.setPostFilter(query);
        }

        if (search.getScroll()!= null) {
            if (search.getScroll().getTimeout() != null) {
                request.setScroll(search.getScroll().getTimeout());
            }
        }

        if (search.getFields() != null) {
            request.setFetchSource(search.getFields(), new String[] {"content"} );
        }

        if (search.getScroll() != null) {
            request.addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC);
        }
        else {
            if (search.getOrder() != null) {
                final Map<String, Set<String>> fields = getFields("asset");
                for (AssetSearchOrder searchOrder : search.getOrder()) {
                    SortOrder sortOrder = searchOrder.getAscending() ? SortOrder.ASC : SortOrder.DESC;
                    // Make sure to use .raw for strings
                    if (!searchOrder.getField().endsWith(".raw") &&
                            fields.get("string").contains(searchOrder.getField()))  {
                        searchOrder.setField(searchOrder.getField() + ".raw");
                    }
                    request.addSort(searchOrder.getField(), sortOrder);
                }
            }
            else {
                // The default sort is always by score, so people can't
                // screw it up too badly.
                request.addSort(SortParseElement.SCORE_FIELD_NAME, SortOrder.DESC);
                for (Map.Entry<String, SortOrder> entry: defaultSortFields.entrySet()) {
                    request.addSort(entry.getKey(), entry.getValue());
                }
            }
        }
        return request;
    }

    private Map<String, Object> getAggregations(AssetSearch search) {
        if (search.getAggs() == null) {
            return null;
        }
        Map<String, Object> result = Maps.newHashMap();
        for (Map.Entry<String, Map<String, Object>> entry: search.getAggs().entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @Override
    public QueryBuilder getQuery(AssetSearch search) {
        return getQuery(search, Sets.newHashSet(), true, false);
    }

    private QueryBuilder getQuery(AssetSearch search, Set<Integer> linkedFolders, boolean perms, boolean postFilter) {

        QueryBuilder permsQuery = SecurityUtils.getPermissionsFilter();
        if (search == null) {
            if (permsQuery == null) {
                return QueryBuilders.matchAllQuery();
            }
            else {
                return permsQuery;
            }
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        BoolQueryBuilder assetBool = QueryBuilders.boolQuery();

        if (perms && permsQuery != null) {
            query.must(permsQuery);
        }

        if (search.isQuerySet()) {
            assetBool.must(getQueryStringQuery(search));
        }

        AssetFilter filter = search.getFilter();
        if (filter != null) {
            applyFilterToQuery(filter, assetBool, linkedFolders);
        }

        AssetFilter elementFilter = search.getElementFilter();
        if (elementFilter != null) {
            BoolQueryBuilder elementBool = QueryBuilders.boolQuery();
            applyFilterToQuery(elementFilter, elementBool, null);
            query.should(QueryBuilders.hasChildQuery("element", elementBool)
                    .scoreMode("max")
                    .maxChildren(1)
                    .innerHit(new QueryInnerHitBuilder().setSize(1)));
        }

        // Folders apply their post filter, but the main search// applies the post filter in the SearchRequest.
        // Aggs will be limited to the folders (correct), but
        // not to the filters in the top-level search.
        if (postFilter) {
            filter = search.getPostFilter();
            if (filter != null) {
                applyFilterToQuery(filter, assetBool, linkedFolders);
            }
        }

        if (assetBool.hasClauses()) {
            query.should(assetBool);
        }

        return query;
    }

    private void linkQuery(BoolQueryBuilder query, AssetFilter filter, Set<Integer> linkedFolders) {

        BoolQueryBuilder staticBool = QueryBuilders.boolQuery();

        Map<String, List<Object>> links = filter.getLinks();
        for (Map.Entry<String, List<Object>> link: links.entrySet()) {
            if (link.getKey().equals("folder")) {
                continue;
            }
            staticBool.should(QueryBuilders.termsQuery("links." + link.getKey(), link.getValue()));
        }

        /*
         * Now do the recursive bit on just folders.
         */
        if (links.containsKey("folder")) {

            Set<Integer> folders = links.get("folder")
                    .stream()
                    .map(f->Integer.valueOf(f.toString()))
                    .filter(f->!linkedFolders.contains(f))
                    .collect(Collectors.toSet());

            boolean recursive = filter.getRecursive() == null ? true : filter.getRecursive();

            if (recursive) {
                Set<Integer> childFolders = Sets.newHashSetWithExpectedSize(64);

                for (Folder folder : folderService.getAllDescendants(
                        folderService.getAll(folders), true, true)) {

                    if (linkedFolders.contains(folder.getId())) {
                        continue;
                    }
                    linkedFolders.add(folder.getId());

                    /**
                     * Not going to allow people to add assets manually
                     * to smart folders, unless its to the smart query itself.
                     */
                    if (folder.getSearch() != null) {
                        folder.getSearch().setAggs(null);
                        staticBool.should(getQuery(folder.getSearch(), linkedFolders, false, true));
                    }

                    /**
                     * We don't allow dyhi folders to have manual entries.
                     */
                    if (folder.getDyhiId() == null && !folder.isDyhiRoot()) {
                        childFolders.add(folder.getId());
                        if (childFolders.size() >= 1024) {
                            break;
                        }
                    }
                }

                if (!childFolders.isEmpty()) {
                    staticBool.should(QueryBuilders.termsQuery("links.folder", childFolders));
                }
            }
            else {
                staticBool.should(QueryBuilders.termsQuery("links.folder", folders));
            }
        }

        query.must(staticBool);
    }

    private QueryBuilder getQueryStringQuery(AssetSearch search) {
        Map<String, Float> queryFields;
        if (JdbcUtils.isValid(search.getQueryFields())) {
            queryFields = search.getQueryFields();
        }
        else {
            queryFields = getQueryFields();
        }

        QueryStringQueryBuilder qstring = QueryBuilders.queryStringQuery(search.getQuery());
        qstring.allowLeadingWildcard(false);
        qstring.lenient(true); // ignores qstring errors
        for (Map.Entry<String,Float> f: queryFields.entrySet()) {
            qstring.field(f.getKey(), f.getValue());
        }
        return qstring;

    }

    /**
     * Apply the given filter to the overall boolean query.
     *
     * @param filter
     * @param query;
     * @return
     */
    private void applyFilterToQuery(AssetFilter filter, BoolQueryBuilder query, Set<Integer> linkedFolders) {

        if (filter.getLinks() != null) {
            linkQuery(query, filter, linkedFolders);
        }

        if (filter.getColors() != null) {
            for (Map.Entry<String, List<ColorFilter>> entry : filter.getColors().entrySet()) {
                for (ColorFilter color: entry.getValue()) {
                    String field = entry.getKey();
                    BoolQueryBuilder colorFilter = QueryBuilders.boolQuery();
                    colorFilter.must(QueryBuilders.rangeQuery(field.concat(".ratio"))
                            .gte(color.getMinRatio()).lte(color.getMaxRatio()));
                    colorFilter.must(QueryBuilders.rangeQuery(field.concat(".hue"))
                            .gte(color.getHue() - color.getHueRange())
                            .lte(color.getHue() + color.getHueRange()));
                    colorFilter.must(QueryBuilders.rangeQuery(field.concat(".saturation"))
                            .gte(color.getSaturation() - color.getSaturationRange())
                            .lte(color.getSaturation() + color.getSaturationRange()));
                    colorFilter.must(QueryBuilders.rangeQuery(field.concat(".brightness"))
                            .gte(color.getBrightness() - color.getBrightnessRange())
                            .lte(color.getBrightness() + color.getBrightnessRange()));

                    QueryBuilder colorFilterBuilder = QueryBuilders.nestedQuery(field,
                            colorFilter);
                    query.must(colorFilterBuilder);
                }
            }
        }

        if (filter.getPrefix() != null) {

            BoolQueryBuilder prefixMust = QueryBuilders.boolQuery();
            // models elasticsearch, the Map<String,Object> allows for a boost property
            for (Map.Entry<String, Map<String,Object>> e : filter.getPrefix().entrySet()) {
                QueryBuilder prefixFilter =
                        QueryBuilders.prefixQuery(e.getKey(),
                                (String) e.getValue().get("prefix")).boost(
                                        ((Double)e.getValue().getOrDefault(
                                                "boost", 1.0)).floatValue());

                prefixMust.should(prefixFilter);
            }
            query.must(prefixMust);
        }

        if (filter.getExists() != null) {
            for (String term : filter.getExists()) {
                QueryBuilder existsFilter = QueryBuilders.existsQuery(term);
                query.must(existsFilter);
            }
        }

        if (filter.getMissing() != null) {
            for (String term : filter.getMissing()) {
                QueryBuilder missingFilter = QueryBuilders.missingQuery(term);
                query.must(missingFilter);
            }
        }

        if (filter.getTerms()!= null) {
            for (Map.Entry<String, List<Object>> term : filter.getTerms().entrySet()) {
                QueryBuilder termsQuery = QueryBuilders.termsQuery(term.getKey(), term.getValue());
                query.must(termsQuery);
            }
        }

        if (filter.getRange() != null) {
            for (Map.Entry<String, RangeQuery> entry: filter.getRange().entrySet()) {
                String field = entry.getKey();
                RangeQuery rq = entry.getValue();
                RangeQueryBuilder rqb = new RangeQueryBuilder(field);

                for (Field f: RangeQuery.class.getDeclaredFields()) {
                    try {
                        Method m = RangeQueryBuilder.class.getMethod(f.getName(), f.getType());
                        Object v = f.get(rq);
                        if (v == null) {
                            continue;
                        }
                        m.invoke(rqb, v);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                                "RangeQueryBuilder has no '" + f.getName() + "' method");
                    }
                }
                query.must(rqb);
            }
        }

        if (filter.getScripts() != null) {
            for (AssetScript script : filter.getScripts()) {
                QueryBuilder scriptFilterBuilder = QueryBuilders.scriptQuery(new Script(
                        script.getScript(), ScriptService.ScriptType.INLINE, script.getType(), script.getParams()));
                query.must(scriptFilterBuilder);
            }
        }

        if (filter.getSimilarity() != null) {
            handleHammingFilter(filter.getSimilarity(), query);
        }

        // Recursively add bool sub-filters for must, must_not and should
        if (filter.getMust() != null) {
            for (AssetFilter f : filter.getMust()) {
                BoolQueryBuilder must = QueryBuilders.boolQuery();
                this.applyFilterToQuery(f, must, linkedFolders);
                query.must(must);
            }
        }

        if (filter.getMustNot() != null) {
            for (AssetFilter f : filter.getMustNot()) {
                BoolQueryBuilder mustNot = QueryBuilders.boolQuery();
                this.applyFilterToQuery(f, mustNot, linkedFolders);
                query.mustNot(mustNot);
            }
        }

        if (filter.getShould() != null) {
            for (AssetFilter f : filter.getShould()) {
                BoolQueryBuilder should = QueryBuilders.boolQuery();
                this.applyFilterToQuery(f, should, linkedFolders);
                query.should(should);
            }
        }
    }

    private void handleHammingFilter(Map<String, SimilarityFilter> filters, BoolQueryBuilder query) {

        BoolQueryBuilder hammingBool = QueryBuilders.boolQuery();
        query.must(hammingBool);

        for (Map.Entry<String, SimilarityFilter> entry : filters.entrySet()) {
            SimilarityFilter filter = entry.getValue();
            String field = entry.getKey();

            /**
             * Resolve any asset Ids in the hash list.
             */

            List<String> hashes = Lists.newArrayList();
            List<Float> weights = Lists.newArrayList();

            for (SimilarityFilter.SimilarityHash hash: filter.getHashes()) {
                String hashValue = hash.getHash();
                if (JdbcUtils.isUUID(hashValue)) {
                    hashValue = assetDao.getFieldValue(hashValue, field);
                }

                if (hashValue != null) {
                    hashes.add(hashValue);
                    weights.add(hash.getWeight() == null ? 1.0f : hash.getWeight());
                }
            }

            Map<String, Object> args = Maps.newHashMap();
            args.put("field", field);
            args.put("hashes", hashes);
            args.put("weights", weights);
            args.put("minScore", filter.getMinScore());

            FunctionScoreQueryBuilder fsqb = QueryBuilders.functionScoreQuery(
                    ScoreFunctionBuilders.scriptFunction(new Script(
                            "hammingDistance", ScriptService.ScriptType.INLINE, "native",
                            args)));

            fsqb.setMinScore(filter.getMinScore() / 100.0f);
            fsqb.boostMode("sum");
            fsqb.scoreMode("multiply");
            hammingBool.should(fsqb);
        }
    }

    private final LoadingCache<String, Map<String, Set<String>>> fieldMapCache = CacheBuilder.newBuilder()
            .maximumSize(2)
            .initialCapacity(3)
            .concurrencyLevel(1)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build(new CacheLoader<String, Map<String, Set<String>>>() {
                public Map<String, Set<String>>load(String key) throws Exception {
                    return getFieldMap(key);
                }
            });

    private Map<String, Set<String>> getFieldMap(String type) {
            Set<String> hiddenFields = fieldDao.getHiddenFields();
            Map<String, Set<String>> result = Maps.newHashMap();
            result.put("string", Sets.newHashSet());
            result.put("date", Sets.newHashSet());
            result.put("integer", Sets.newHashSet());
            result.put("long", Sets.newHashSet());
            result.put("point", Sets.newHashSet());
            result.put("keywords-auto", Sets.newHashSet());
            result.put("keywords", Sets.newHashSet());

            boolean autoKeywords = properties.getBoolean("archivist.search.keywords.auto.enabled");
            Set<String> autoKeywordFieldNames = null;
            if (autoKeywords) {
                autoKeywordFieldNames = ImmutableSet.copyOf(
                        properties.getList("archivist.search.keywords.auto.fields"));
            }

            Map<String, Object> fields = properties.getMap(PROP_PREFIX_KEYWORD_FIELD);
            if (fields != null) {
                fields.forEach((k, v) -> result.get("keywords").add(
                        k.replace(PROP_PREFIX_KEYWORD_FIELD, "")));
            }

            ClusterState cs = client.admin().cluster()
                    .prepareState()
                    .setIndices(alias)
                    .execute().actionGet().getState();
            for (String index: cs.getMetaData().concreteAllOpenIndices()) {
                IndexMetaData imd = cs.getMetaData().index(index);
                MappingMetaData mdd = imd.mapping(type);
                try {
                    getList(result, "", mdd.getSourceAsMap(), autoKeywordFieldNames, hiddenFields);
                } catch (IOException e) {
                    throw new ArchivistException(e);
                }
            }
            return result;
    }

    /*
     * TODO: Move all field stuff to asset service.
     */

    @Override
    public void invalidateFields() {
        fieldMapCache.invalidateAll();
    }

    @Override
    public boolean updateField(HideField value) {
        try {
            if (value.isHide()) {
                return fieldDao.hideField(value.getField(), value.isManual());
            } else {
                return fieldDao.unhideField(value.getField());
            }
        } finally {
            invalidateFields();
        }
    }

    @Override
    public Map<String, Set<String>> getFields(String type) {
        try {
            return fieldMapCache.get(type);
        } catch (Exception e) {
            logger.warn("Failed to get fields: ", e);
            return ImmutableMap.of();
        }
    }

    private static final Map<String,String> NAME_TYPE_OVERRRIDES = ImmutableMap.of(
            "point", "point",
            "byte", "hash",
            "hash", "hash");

    private void getList(Map<String, Set<String>> result,
                         String fieldName,
                         Map<String, Object> mapProperties,
                         Set<String> autoKeywordFieldNames,
                         Set<String> hiddenFieldNames) {

        Map<String, Object> map = (Map<String, Object>) mapProperties.get("properties");
        for (String key : map.keySet()) {
            Map<String, Object> item = (Map<String, Object>) map.get(key);

            if (!fieldName.isEmpty()) {
                if (hiddenFieldNames.contains(fieldName)) {
                    continue;
                }
            }

            if (item.containsKey("type")) {
                String type = (String) item.get("type");
                type = NAME_TYPE_OVERRRIDES.getOrDefault(key, type);

                Set<String> fields = result.get(type);
                if (fields == null) {
                    fields = new TreeSet<>();
                    result.put(type, fields);
                }
                String fqfn = String.join("", fieldName, key);

                if (hiddenFieldNames.contains(fqfn)) {
                    continue;
                }

                fields.add(fqfn);

                /*
                 * If the last part of the field is set as an auto-keywords field
                 * then it gets added to the keywords-auto list.
                 */
                if (autoKeywordFieldNames.contains(key.toLowerCase()) || autoKeywordFieldNames.contains(fqfn)) {
                    result.get("keywords-auto").add(fqfn);
                }


            } else {
                getList(result, String.join("", fieldName, key, "."),
                        item, autoKeywordFieldNames, hiddenFieldNames);
            }
        }
    }

    /**
     * The properties prefix used to define keywords fields.
     */
    private static final String PROP_PREFIX_KEYWORD_FIELD = "archivist.search.keywords.static.fields";

    @Override
    public Map<String, Float> getQueryFields() {

        Set<String> autoFields = getFields("asset").get("keywords-auto");
        String staticFieldsJson = properties.getString(PROP_PREFIX_KEYWORD_FIELD);

        ImmutableMap.Builder < String, Float > builder = ImmutableMap.builder();
        if (JdbcUtils.isValid(staticFieldsJson)) {
            try {
                builder.putAll(Json.deserialize(staticFieldsJson,
                        new TypeReference<Map<String, Float>>() {}));
            } catch (Exception e) {
                logger.warn("Failed to parse static field setting: {}", staticFieldsJson);
            }
        }
        autoFields.forEach(v->builder.put(v, 1.0f));
        return builder.build();
    }

    private void initializeDefaultQuerySettings() {
        /**
         * Setup default sort.
         */
        defaultSortFields = Maps.newLinkedHashMap();
        List<String> sortFields =
                properties.getList("archivist.search.sortFields");

        for (String field: sortFields) {
            List<String> e = Splitter.on(":").omitEmptyStrings().trimResults().splitToList(field);
            if (e.size() != 2) {
                logger.warn("Failed to add default sort option: {}, to many values.(should be field:direction)", field);
            }
            defaultSortFields.put(e.get(0), SortOrder.valueOf(e.get(1)));
        }
        logger.info("Default sort fields: {}", defaultSortFields);
    }

    @Override
    public List<String> analyzeQuery(AssetSearch search) {
        if (search.getQuery() == null || search.getQuery().isEmpty()) {
            return ImmutableList.of();
        }

        return client.admin().indices().prepareAnalyze(
                search.getQuery())
                .setIndex("archivist")
                .get().getTokens().stream().map(e->e.getTerm()).collect(Collectors.toList());
    }

    /**
     * If a field doesn't end with .raw, concat .raw
     *
     * @param field
     * @return
     */
    private static String dotRawMe(String field) {
        if (field.endsWith(".raw")) {
            return field;
        }
        return field.concat(".raw");
    }
}
