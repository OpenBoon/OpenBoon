package com.zorroa.archivist.service;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.exception.ArchivistException;
import com.zorroa.sdk.exception.ZorroaReadException;
import com.zorroa.sdk.search.*;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

    @Override
    public SearchResponse search(AssetSearch search) {
        SearchResponse rsp =  buildSearch(search).get();
        return rsp;
    }

    @Override
    public long count(AssetSearch builder) {
        return buildSearch(builder).setSize(0).get().getHits().getTotalHits();
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
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(60000))
                .setQuery(getQuery(search))
                .setSize(100).execute().actionGet();

        if (rsp.getHits().getTotalHits() > maxResults) {
            throw new ZorroaReadException("Asset search has returned more than " + maxResults + " results.");
        }
        return new ScanAndScrollAssetIterator(client, rsp);
    }

    @Override
    public PagedList<Asset> search(Paging page, AssetSearch search) {
        /**
         * Only log valid searches (the ones that are not for the whole repo)
         * since otherwise it creates a lot of logs of empty searches.
         */
        if (!search.isEmpty()) {
            logService.log(new LogSpec().build(LogAction.Search, search));
        }
        return assetDao.getAll(page, buildSearch(search));
    }

    private SearchRequestBuilder buildSearch(AssetSearch search) {

        SearchRequestBuilder request = client.prepareSearch(alias)
                .setTypes("asset")
                .setQuery(getQuery(search));

        if (search.getFields() != null) {
            request.setFetchSource(search.getFields(), new String[] { "links", "permissions"} );
        }

        Paging page = new Paging(search.getPage(), search.getSize());
        request.setFrom(page.getFrom());
        request.setSize(page.getSize());

        if (search.getOrder() != null) {
            for (AssetSearchOrder searchOrder : search.getOrder()) {
                SortOrder sortOrder = searchOrder.getAscending() ? SortOrder.ASC : SortOrder.DESC;
                request.addSort(searchOrder.getField(), sortOrder);
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
                .setSearchType(SearchType.COUNT);
        return aggregation;
    }

    private QueryBuilder getQuery(AssetSearch search) {
        return getQuery(search, true);
    }

    private QueryBuilder getQuery(AssetSearch search, boolean perms) {
        if (search == null) {
            return QueryBuilders.filteredQuery(
                    QueryBuilders.matchAllQuery(), SecurityUtils.getPermissionsFilter());
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (perms) {
            query.must(SecurityUtils.getPermissionsFilter());
        }

        if (search.isQuerySet()) {
            query.must(getQueryStringQuery(search));
        }

        AssetFilter filter = search.getFilter();
        if (filter != null) {
            applyFilterToQuery(filter, query);
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

            Set<String> childFolders = Sets.newHashSet();
            for (Folder folder : folderService.getAllDescendants(
                    folderService.getAll(folders), true, true)) {

                /**
                 * Not going to allow people to add assets manually
                 * to smart folders, unless its to the smart query itself.
                 */
                if (folder.getSearch() != null) {
                    staticBool.should(getQuery(folder.getSearch(), false));
                }

                /**
                 * We don't allow dyhi folders to have manual entries.
                 */
                if (folder.getDyhiId() == null && !folder.isDyhiRoot()) {
                    childFolders.add(String.valueOf(folder.getId()));
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
         * Note: fuzzy defaults to true.
         */
        String query = search.getQuery();
        boolean fuzzy = search.getFuzzy() != null ? search.getFuzzy() : true;
        if (fuzzy && query != null) {
            StringBuilder sb = new StringBuilder(query.length() + 10);
            for (String part: Splitter.on(" ").omitEmptyStrings().trimResults().split(query)) {
                sb.append(part);
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
        qstring.field("keywords.all");
        qstring.field("keywords.all.raw", 2);
        qstring.allowLeadingWildcard(false);
        qstring.lenient(true);
        qstring.fuzziness(Fuzziness.AUTO);
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
    }

    @Override
    public Map<String, Set<String>> getFields() {
        Map<String, Set<String>> result = Maps.newHashMapWithExpectedSize(16);
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
}
