package com.zorroa.archivist.service;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.*;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.client.exception.ArchivistException;
import com.zorroa.sdk.client.exception.ArchivistReadException;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.search.*;
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
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;
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
    LogService logService;

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Autowired
    ApplicationProperties properties;

    private Map<String, Float> defaultQueryFields =
            ImmutableMap.of("keywords.all", 1.0f, "keywords.all.raw", 2.0f);

    private Map<String, SortOrder> defaultSortFields = Maps.newLinkedHashMap();

    @PostConstruct
    public void init() {
        initializeDefaultQueryFields();
    }

    @Override
    public SearchResponse search(AssetSearch search) {
        SearchResponse rsp =  buildSearch(search)
                .setFrom(search.getFrom() == null ? 0 : search.getFrom())
                .setSize(search.getSize() == null ? 10 : search.getSize()).get();
        return rsp;
    }

    @Override
    public long count(AssetSearch builder) {
        return buildSearch(builder).setSize(0).get().getHits().getTotalHits();
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
        if (search != null && search.getFilter() != null) {
            search.getFilter().addToLinks("folder", String.valueOf(folder.getId()));
            return count(search);
        }
        else {
            search = new AssetSearch();
            search.addToFilter().addToLinks("folder", String.valueOf(folder.getId()));
            return count(search);
        }
    }

    @Override
    public SuggestResponse suggest(AssetSuggestBuilder builder) {
        return buildSuggest(builder).get();
    }

    @Override
    public SearchResponse aggregate(AssetAggregateBuilder builder) {
        return buildAggregate(builder).get();
    }

    @Override
    public Iterable<Asset> scanAndScroll(AssetSearch search, int maxResults) {
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
    public PagedList<Asset> search(Pager page, AssetSearch search) {
        /**
         * If the search is not empty (its a valid search) and the page
         * number is 1, then log the search.
         */
        if (isSearchLogged(page, search)) {
            logService.logAsync(new LogSpec().build(LogAction.Search, search));
        }

        if (search.getScroll() != null) {
            Scroll scroll = search.getScroll();
            if (scroll.getId() != null) {
                PagedList<Asset> result = assetDao.getAll(scroll.getId(), scroll.getTimeout());
                if (result.size() == 0) {
                    client.prepareClearScroll().addScrollId(scroll.getId());
                }
                return result;
            }
        }

        return assetDao.getAll(page, buildSearch(search));
    }

    @Override
    public void search(Pager page, AssetSearch search, OutputStream stream) throws IOException {
        /**
         * If the search is not empty (its a valid search) and the page
         * number is 1, then log the search.
         */
        if (isSearchLogged(page, search)) {
            logService.logAsync(new LogSpec().build(LogAction.Search, search));
        }

        assetDao.getAll(page, buildSearch(search), stream);
    }


    @Override
    public PagedList<Asset> scroll(String id, String timeout) {
        /**
         * Only log valid searches (the ones that are not for the whole repo)
         * since otherwise it creates a lot of logs of empty searches.
         */
        PagedList<Asset> result =  assetDao.getAll(id, timeout);
        if (result.size() == 0) {
            client.prepareClearScroll().addScrollId(id);
        }
        return result;
    }

    @Override
    public SearchRequestBuilder buildSearch(AssetSearch search) {

        SearchRequestBuilder request = client.prepareSearch(alias)
                .setTypes("asset")
                .setPreference(SecurityUtils.getUsername())
                .setQuery(getQuery(search));

        if (search.getAggs() != null) {
            request.setAggregations(getAggregations(search));
        }

        if (search.getPostFilter() != null) {
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            applyFilterToQuery(search.getPostFilter(), query);
            request.setPostFilter(query);
        }

        if (search.getScroll()!= null) {
            if (search.getScroll().getTimeout() != null) {
                request.setScroll(search.getScroll().getTimeout());
            }
        }

        if (search.getFields() != null) {
            request.setFetchSource(search.getFields(), new String[] {} );
        }

        if (search.getScroll() != null) {
            request.addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC);
        }
        else {
            if (search.getOrder() != null) {
                final Map<String, Set<String>> fields = getFields();
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

    private SuggestRequestBuilder buildSuggest(AssetSuggestBuilder builder) {
        // FIXME: We need to use builder.search in here somehow!
        CompletionSuggestionBuilder completion = new CompletionSuggestionBuilder("completions")
                .text(builder.getText())
                .field("keywords.suggest");
        SuggestRequestBuilder suggest = client.prepareSuggest(alias)
                .addSuggestion(completion);
        return  suggest;
    }

    private SearchRequestBuilder buildAggregate(AssetAggregateBuilder builder) {
        SearchRequestBuilder aggregation = client.prepareSearch(alias)
                .setTypes("asset")
                .setQuery(getQuery(builder.getSearch()))
                .setAggregations(builder.getAggregations())
                .setSize(0);
        return aggregation;
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

    private QueryBuilder getQuery(AssetSearch search) {
        return getQuery(search, true, false);
    }

    private QueryBuilder getQuery(AssetSearch search, boolean perms, boolean postFilter) {

        QueryBuilder permsQuery = SecurityUtils.getPermissionsFilter();
        if (search == null) {
            if (permsQuery == null) {
                return QueryBuilders.matchAllQuery();
            }
            else {
                return QueryBuilders.filteredQuery(
                        QueryBuilders.matchAllQuery(), permsQuery);
            }
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();

        if (perms && permsQuery != null) {
            query.must(permsQuery);
        }

        if (search.isQuerySet()) {
            query.must(getQueryStringQuery(search));
        }

        AssetFilter filter = search.getFilter();
        if (filter != null) {
            applyFilterToQuery(filter, query);
        }

        // Folders apply their post filter, but the main search
        // applies the post filter in the SearchRequest.
        // Aggs will be limited to the folders (correct), but
        // not to the filters in the top-level search.
        if (postFilter) {
            filter = search.getPostFilter();
            if (filter != null) {
                applyFilterToQuery(filter, query);
            }
        }

        return query;
    }

    private void linkQuery(BoolQueryBuilder query, AssetFilter filter) {

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

            List<Integer> folders = links.get("folder")
                    .stream().map(f->Integer.valueOf(f.toString())).collect(Collectors.toList());

            Set<String> childFolders = Sets.newHashSetWithExpectedSize(64);
            for (Folder folder : folderService.getAllDescendants(
                    folderService.getAll(folders), true, true)) {

                /**
                 * Not going to allow people to add assets manually
                 * to smart folders, unless its to the smart query itself.
                 */
                if (folder.getSearch() != null) {
                    staticBool.should(getQuery(folder.getSearch(), false, true));
                }

                /**
                 * We don't allow dyhi folders to have manual entries.
                 */
                if (folder.getDyhiId() == null && !folder.isDyhiRoot()) {
                    childFolders.add(String.valueOf(folder.getId()));
                    if (childFolders.size() >= 1024) {
                        break;
                    }
                }
            }

            if (!childFolders.isEmpty()) {
                staticBool.should(QueryBuilders.termsQuery("links.folder", childFolders));
            }
        }

        query.must(staticBool);
    }

    private QueryBuilder getQueryStringQuery(AssetSearch search) {

        /**
         * Note: fuzzy defaults to false.
         */
        String query = search.getQuery();


        /**
         * Default fuzzy to off.
         **/
        boolean fuzzy = search.getFuzzy() != null ? search.getFuzzy() : false;

        if (fuzzy && query != null) {
            StringBuilder sb = new StringBuilder(query.length() + 10);
            for (String part: Splitter.on(" ").omitEmptyStrings().trimResults().split(query)) {
                sb.append(part);
                /*
                 * Append the fuzzy search character to words ending with
                 * alphanumeric characters, excluding ES logical keywords.
                 */
                if (!part.matches(".*[a-zA-Z0-9]$") ||
                        part.matches("^.*?(AND|OR|NOT).*$")) {
                    continue;
                }
                if (part.endsWith("~")) {
                    sb.append(" ");
                }
                else {
                    sb.append("~ ");
                }
            }
            sb.deleteCharAt(sb.length()-1);
            query = sb.toString();
        }

        QueryStringQueryBuilder qstring = QueryBuilders.queryStringQuery(query);
        Map<String, Float> queryFields = null;

        if (JdbcUtils.isValid(search.getQueryFields())) {
            queryFields = search.getQueryFields();
        }

        if (!JdbcUtils.isValid(queryFields)) {
            queryFields = defaultQueryFields;
        }

        queryFields.forEach((k,v)-> qstring.field(k, v));
        qstring.allowLeadingWildcard(false);
        // ignores qstring errors
        qstring.lenient(true);
        return qstring;
    }


    /**
     * Apply the given filter to the overall boolean query.
     *
     * @param filter
     * @param query;
     * @return
     */
    private void applyFilterToQuery(AssetFilter filter, BoolQueryBuilder query) {

        if (filter.getLinks() != null) {
            linkQuery(query, filter);
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
            for (Map.Entry<String, Map<String,Object>> e : filter.getPrefix().entrySet()) {
                QueryBuilder prefixFilter = QueryBuilders.prefixQuery(e.getKey(), (String) e.getValue().get("prefix"));
                query.must(prefixFilter);
            }
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

        if (filter.getHamming() != null) {
            Map<String, Object> args = Maps.newHashMap();
            args.put("field", dotRawMe(filter.getHamming().getField()));
            args.put("hashes", filter.getHamming().getHashes());
            args.put("minScore", filter.getHamming().getMinScore());
            if (filter.getHamming().getWeights() != null) args.put("weights", filter.getHamming().getWeights());
            FunctionScoreQueryBuilder fsqb = QueryBuilders.functionScoreQuery(ScoreFunctionBuilders.scriptFunction(new Script(
                    "hammingDistance", ScriptService.ScriptType.INLINE, "native",
                    args)));
            fsqb.setMinScore(filter.getHamming().getMinScore());
            fsqb.scoreMode("max");
            fsqb.boostMode("replace");
            query.must(fsqb);
        }

        // Recursively add bool sub-filters for must, must_not and should
        if (filter.getMust() != null) {
            for (AssetFilter f : filter.getMust()) {
                BoolQueryBuilder must = QueryBuilders.boolQuery();
                this.applyFilterToQuery(f, must);
                query.must(must);
            }
        }

        if (filter.getMustNot() != null) {
            for (AssetFilter f : filter.getMustNot()) {
                BoolQueryBuilder mustNot = QueryBuilders.boolQuery();
                this.applyFilterToQuery(f, mustNot);
                query.mustNot(mustNot);
            }
        }

        if (filter.getShould() != null) {
            for (AssetFilter f : filter.getShould()) {
                BoolQueryBuilder should = QueryBuilders.boolQuery();
                this.applyFilterToQuery(f, should);
                query.should(should);
            }
        }
    }

    /**
     * Cache this as much as possible.  We'll eventually want to manually expire this,
     * but we'll need a new type of Supplier.
     */
    private final Supplier<Map<String, Set<String>>> fieldMapCache
            = Suppliers.memoizeWithExpiration(fieldMapSupplier(), 1, TimeUnit.MINUTES);

    private final Supplier<Map<String, Set<String>>> fieldMapSupplier() {
        return () -> {
            Map<String, Set<String>> result = Maps.newHashMap();
            result.put("string", Sets.newHashSet());
            result.put("date", Sets.newHashSet());
            result.put("integer", Sets.newHashSet());
            result.put("point", Sets.newHashSet());

            ClusterState cs = client.admin().cluster().prepareState().setIndices(alias).execute().actionGet().getState();
            for (String index: cs.getMetaData().concreteAllOpenIndices()) {
                IndexMetaData imd = cs.getMetaData().index(index);
                MappingMetaData mdd = imd.mapping("asset");
                try {
                    getList(result, "", mdd.getSourceAsMap());
                } catch (IOException e) {
                    throw new ArchivistException(e);
                }
            }
            return result;
        };
    }

    @Override
    public Map<String, Set<String>> getFields() {
        return fieldMapCache.get();
    }

    private static final Set<String> NAME_TYPE_OVERRRIDES = ImmutableSet.of("point");

    private static void getList(Map<String, Set<String>> result, String fieldName, Map<String, Object> mapProperties) {
        Map<String, Object> map = (Map<String, Object>) mapProperties.get("properties");
        for (String key : map.keySet()) {
            Map<String, Object> item = (Map<String, Object>) map.get(key);

            if (item.containsKey("type")) {
                String type = (String) item.get("type");
                if (NAME_TYPE_OVERRRIDES.contains(key)) {
                    type = key;
                }
                Set<String> fields = result.get(type);
                if (fields == null) {
                    fields = new TreeSet<>();
                    result.put(type, fields);
                }
                fields.add(String.join("", fieldName, key));
            } else {
                getList(result, String.join("", fieldName, key, "."), item);
            }
        }
    }

    private void initializeDefaultQueryFields() {
        Map<String, Object> queryFieldProps =
                properties.getMap("archivist.search.queryFields");

        if (!queryFieldProps.isEmpty()) {
            /**
             * Using ImmutableMap.Builder to ensure this default cannot
             * be modified by accident.
             */
            ImmutableMap.Builder<String, Float> builder = ImmutableMap.builder();
            queryFieldProps.forEach((k,v)-> builder.put(
                    k.replace("archivist.search.queryFields.",""),
                    Float.valueOf(v.toString())));
            defaultQueryFields = builder.build();
        }
        logger.info("Default search fields: {}", defaultQueryFields);

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
