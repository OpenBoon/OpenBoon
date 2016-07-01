package com.zorroa.archivist.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.ScanAndScrollAssetIterator;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.exception.ArchivistException;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
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
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by chambers on 9/25/15.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    FolderService folderService;

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Override
    public SearchResponse search(AssetSearch search) {
        return buildSearch(search).get();
    }

    @Override
    public CountResponse count(AssetSearch builder) {
        return buildCount(builder).get();
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
    public long getTotalFileSize(AssetSearch search) {
        Sum sum = client.prepareSearch(alias)
                .setTypes("asset")
                .setQuery(getQuery(search))
                .addAggregation(AggregationBuilders.sum("totalFileSize").field("source.fileSize"))
                .setSearchType(SearchType.COUNT)
                .get().getAggregations().get("totalFileSize");

        return (long) sum.getValue();
    }

    public Iterable<Asset> scanAndScroll(AssetSearch search) {

        SearchResponse rsp = client.prepareSearch(alias)
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(60000))
                .setQuery(getQuery(search))
                .setSize(100).execute().actionGet();

        return new ScanAndScrollAssetIterator(client, rsp);
    }

    private SearchRequestBuilder buildSearch(AssetSearch search) {

        SearchRequestBuilder request = client.prepareSearch(alias)
                .setTypes("asset")
                .setQuery(getQuery(search));

        if (search.getFields() != null) {
            request.setFetchSource(search.getFields(), new String[]{});
        }

        if (search.getFrom() != null) {
            request.setFrom(search.getFrom());
        }

        if (search.getSize() != null) {
            request.setSize(search.getSize());
        }

        /*
         * TODO: alternative sorting and paging here.
         */

        return request;
    }

    private CountRequestBuilder buildCount(AssetSearch search) {
        CountRequestBuilder count = client.prepareCount(alias)
                .setTypes("asset")
                .setQuery(getQuery(search));
        return count;
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
        if (search == null) {
            return QueryBuilders.filteredQuery(
                    QueryBuilders.matchAllQuery(), getFilter(null));
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (search.isQuerySet()) {
            query.must(getQueryStringQuery(search));
        }

        AssetFilter filter = search.getFilter();
        if (filter.getFolderIds() != null) {
            query.must(folderQuery(filter));
        }
        if (filter.getColors() != null) {
            for (ColorFilter color: filter.getColors()) {
                Preconditions.checkNotNull(color.getField(), "The ColorFilter.field was not set.");
                BoolQueryBuilder colorFilter = QueryBuilders.boolQuery();
                colorFilter.must(QueryBuilders.rangeQuery(color.getField().concat(".ratio"))
                        .gte(color.getMinRatio()).lte(color.getMaxRatio()));
                colorFilter.must(QueryBuilders.rangeQuery(color.getField().concat(".hue"))
                        .gte(color.getHue() - color.getHueRange())
                        .lte(color.getHue() + color.getHueRange()));
                colorFilter.must(QueryBuilders.rangeQuery(color.getField().concat(".saturation"))
                        .gte(color.getSaturation() - color.getSaturationRange())
                        .lte(color.getSaturation() + color.getSaturationRange()));
                colorFilter.must(QueryBuilders.rangeQuery(color.getField().concat(".brightness"))
                        .gte(color.getBrightness() - color.getBrightnessRange())
                        .lte(color.getBrightness() + color.getBrightnessRange()));

                QueryBuilder colorFilterBuilder = QueryBuilders.nestedQuery(color.getField(),
                        colorFilter);
                query.must(colorFilterBuilder);
            }
        }
        QueryBuilder filterBuilder = getFilter(filter);
        query.filter(filterBuilder);
        return query;
    }

    // Combine the folder search and filter using SHOULD.
    // Merged into the main query as a MUST above.
    private QueryBuilder folderQuery(AssetFilter filter) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (filter.getFolderIds() != null) {
            Set<Integer> folderIds = Sets.newHashSetWithExpectedSize(64);
            for (Folder folder : folderService.getAllDescendants(
                    folderService.getAll(filter.getFolderIds()), true, true)) {
                if (folder.getSearch() != null) {
                    query.should(getQuery(folder.getSearch()));
                }
                folderIds.add(folder.getId());
            }

            if (!folderIds.isEmpty()) {
                query.should(QueryBuilders.termsQuery("folders", folderIds));
            }
        }

        return query;
    }

    private QueryBuilder getQueryStringQuery(AssetSearch search) {

        String query = search.getQuery();
        if (search.isFuzzy() && query != null) {
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


        /*
         * Note: the default boost is 1
         *
         * With 5 keyword buckets, the boosts work out to:
         * 1 = 0.1
         * 2 = 0.5
         * 3 = 1.0 (DEFAULT)
         * 4 = 1.5
         * 5 = 2.0
         *
         * The indexed fields get 1/2 as much boost.
         *
         * TODO: switch to BigDecmial to deal with rounding errors.
         * TODO: function score query is another option
         */

        qstring.field("keywords.all");
        qstring.field("keywords.all.raw", 2);
        qstring.allowLeadingWildcard(false);
        qstring.lenient(true);
        qstring.fuzziness(Fuzziness.AUTO);
        return qstring;
    }


    /**
     * Builds an "AND" filter based on all the options in the AssetFilter.
     *
     * @param builder
     * @return
     */
    private QueryBuilder getFilter(AssetFilter builder) {
        AndQueryBuilder filter =  QueryBuilders.andQuery();
        filter.add(SecurityUtils.getPermissionsFilter());

        if (builder == null) {
            return filter;
        }

        if (builder.getAssetIds() != null) {
            QueryBuilder assetsFilterBuilder = QueryBuilders.termsQuery("_id", builder.getAssetIds());
            filter.add(assetsFilterBuilder);
        }

        if (builder.getIngestIds() != null) {
            /**
             * An asset can be exported N times so the "exports" field is an array.  For ingest, we record
             * the first ingest ID along with the pipeline, so the value is an embedded object.  Thus, how
             * they are queried is not the same.
             */
            QueryBuilder ingestsFilterBuilder = QueryBuilders.termsQuery("imports.id", builder.getIngestIds());
            filter.add(ingestsFilterBuilder);
        }

        if (builder.getExportIds() != null) {
            QueryBuilder exportsFilterBuilder = QueryBuilders.termsQuery("exports", builder.getExportIds());
            filter.add(exportsFilterBuilder);
        }

        if (builder.getExistFields() != null) {
            for (String term : builder.getExistFields()) {
                QueryBuilder existsFilterBuilder = QueryBuilders.existsQuery(term);
                filter.add(existsFilterBuilder);
            }
        }

        if (builder.getFieldTerms() != null) {
            for (AssetFieldTerms fieldTerms : builder.getFieldTerms()) {
                QueryBuilder termsFilterBuilder = QueryBuilders.termsQuery(fieldTerms.getField(), fieldTerms.getTerms());
                filter.add(termsFilterBuilder);
            }
        }

        if (builder.getFieldRanges() != null) {
            for (AssetFieldRange fieldRange : builder.getFieldRanges()) {
                QueryBuilder rangeFilterBuilder = QueryBuilders.rangeQuery(fieldRange.getField())
                        .gte(fieldRange.getMin())
                        .lt(fieldRange.getMax());
                filter.add(rangeFilterBuilder);
            }
        }

        if (builder.getScripts() != null) {
            for (AssetScript script : builder.getScripts()) {
                QueryBuilder scriptFilterBuilder = QueryBuilders.scriptQuery(new Script(
                        script.getScript(), ScriptService.ScriptType.INLINE, "native", script.getParams()));
                filter.add(scriptFilterBuilder);
            }
        }

        return filter;
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
