package com.zorroa.archivist.repository;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.google.common.collect.Lists;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import com.zorroa.archivist.sdk.util.Json;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class FolderDaoImpl extends AbstractElasticDao implements FolderDao {

    private TimeBasedGenerator uuidGenerator = Generators.timeBasedGenerator();

    @Override
    public String getType() {
        return "folders";
    }

    private static final JsonRowMapper<Folder> MAPPER = (id, version, source) -> {
        Folder folder = Json.deserialize(source, Folder.class);
        folder.setId(id);
        return folder;
    };

    @Override
    public Folder get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    private List<Folder> getFolders(QueryBuilder queryBuilder) {
        List<Folder> result = Lists.newArrayListWithCapacity(32);
        SearchResponse scroll = client.prepareSearch(alias)
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(5000))
                .setQuery(queryBuilder)
                .setSize(100).execute().actionGet();

        while(true) {
            for (SearchHit hit: scroll.getHits().getHits()) {
                try {
                    result.add(MAPPER.mapRow(hit.getId(), hit.getVersion(), hit.source()));
                } catch (Exception e) {
                    // Whatever data we got was unable to be deserialized...maybe bad folder
                    // warn, but move on
                    logger.warn("Unable to deserialize folder Id: {}", hit.getId(), e);
                }
            }

            scroll = client.prepareSearchScroll(scroll.getScrollId())
                    .setScroll(new TimeValue(5000))
                    .execute().actionGet();

            if (scroll.getHits().getHits().length == 0) {
                break;
            }
        }

        Collections.sort(result, (f1, f2) -> f1.getName().compareTo(f2.getName()));
        return result;
    }

    @Override
    public List<Folder> getAll() {
        FilterBuilder filter = FilterBuilders.andFilter(
                FilterBuilders.termFilter("userId", SecurityUtils.getUser().getId()),
                FilterBuilders.termFilter("parentId", Folder.ROOT_ID)
        );

        FilteredQueryBuilder query = QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filter);
        return getFolders(query);
    }

    @Override
    public List<Folder> getAllShared() {
        FilterBuilder filter = FilterBuilders.andFilter(
                FilterBuilders.termFilter("shared", true)
        );

        FilteredQueryBuilder query = QueryBuilders.filteredQuery(
                QueryBuilders.matchAllQuery(), filter);

        // Reset the root folder for the shared folder list.
        List<Folder> folders = getFolders(query);
        folders.forEach(f -> f.setParentId(Folder.ROOT_ID));
        return folders;
    }

    @Override
    public List<Folder> getChildren(Folder folder) {
        return getFolders(QueryBuilders.termQuery("parentId", folder.getId()));
    }

    @Override
    public boolean exists(String parentId, String name) {
        FilterBuilder userFilter = FilterBuilders.termFilter("userId", SecurityUtils.getUser().getId());
        FilterBuilder nameFilter = FilterBuilders.termFilter("name", name);
        FilterBuilder parentFilter = FilterBuilders.termFilter("parentId", parentId);

        CountResponse count = client.prepareCount(alias)
                .setTypes(getType())
                .setQuery(QueryBuilders.filteredQuery(
                        QueryBuilders.matchAllQuery(),
                        FilterBuilders.andFilter(
                                userFilter, parentFilter, nameFilter)))
                .get();
        return count.getCount() > 0;
    }

    @Override
    public Folder create(FolderBuilder builder) {
        /*
         * There is some better way to do this that doesn't require
         * userId to be in the folder builder.
         */
        StringBuilder sb = new StringBuilder(Json.serializeToString(builder));
        sb.deleteCharAt(sb.length()-1);
        sb.append(",\"userId\":");
        sb.append(SecurityUtils.getUser().getId());
        sb.append("}");

        IndexRequestBuilder idxBuilder = client.prepareIndex(alias, getType())
                .setId(uuidGenerator.generate().toString())
                .setOpType(IndexRequest.OpType.CREATE)
                .setSource(sb.toString())
                .setRefresh(true);
        String id = idxBuilder.get().getId();
        return get(id);
    }

    @Override
    public boolean update(Folder folder, FolderBuilder builder) {
        client.prepareUpdate(alias, getType(), folder.getId())
                .setDoc(Json.serializeToString(builder))
                .setRefresh(true)
                .setRetryOnConflict(1)
                .get().isCreated();
        return true;
    }

    @Override
    public boolean delete(Folder folder) {
        DeleteRequestBuilder builder = client.prepareDelete()
                .setIndex(alias)
                .setType(getType())
                .setId(folder.getId())
                .setType("folders")
                .setRefresh(true);
        String id = builder.get().getId();
        return id.equals(folder.getId());
    }
}
