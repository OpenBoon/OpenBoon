package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import com.zorroa.archivist.sdk.util.Json;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.*;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class FolderDaoImpl extends AbstractElasticDao implements FolderDao {

    private TimeBasedGenerator uuidGenerator = Generators.timeBasedGenerator();

    @Override
    public String getType() {
        return "folders";
    }

    private static final JsonRowMapper<Folder> MAPPER = new JsonRowMapper<Folder>() {
        @Override
        public Folder mapRow(String id, long version, byte[] source) {
            try {
                Map<String, Object> data = Json.Mapper.readValue(source, new TypeReference<Map<String, Object>>() {});
                Folder folder = new Folder();
                folder.setId(id);
                folder.setName((String) data.get("name"));
                folder.setUserId((int) data.get("userId"));
                if (data.get("parentId") != null)
                    folder.setParentId((String)data.get("parentId"));
                if (data.get("query") != null)
                    folder.setQuery((String) data.get("query"));
                return folder;
            } catch (IOException e) {
                throw new DataRetrievalFailureException("Failed to parse folder record, " + e, e);
            }
        }
    };

    @Override
    public Folder get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    private List<Folder> getFolders(QueryBuilder queryBuilder) {
        List<Folder> folders = new ArrayList<Folder>();
        for (int i = 0; i < 1000; ++i) {
            final int scrollSize = 1000;
            SearchRequestBuilder search = client.prepareSearch(alias)
                    .setTypes(getType())
                    .setQuery(queryBuilder)
                    .setSize(scrollSize)
                    .setFrom(i * scrollSize);
            List<Folder> folderPage = elastic.query(search, MAPPER);
            folders.addAll(folderPage);
            if (folderPage.size() < scrollSize)
                break;
        }
        return folders;
    }

    @Override
    public List<Folder> getAll(int userId) {
        // Build a filtered query that matches the user and excludes child folders
        TermFilterBuilder userFilter = FilterBuilders.termFilter("userId", userId);
        ExistsFilterBuilder parentFilter = FilterBuilders.existsFilter("parentId");
        BoolFilterBuilder boolFilter = FilterBuilders.boolFilter()
                .must(userFilter)
                .mustNot(parentFilter);
        FilteredQueryBuilder query = QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), boolFilter);
        return getFolders(query);
    }

    @Override
    public List<Folder> getChildren(Folder folder) {
        return getFolders(QueryBuilders.termQuery("parentId", folder.getId()));
    }

    @Override
    public boolean exists(String parentId, String name) {
        FilterBuilder userFilter = FilterBuilders.termFilter("userId", SecurityUtils.getUser().getId());
        FilterBuilder nameFilter = FilterBuilders.termFilter("name", name);
        FilterBuilder parentFilter;

        if (parentId == null) {
            parentFilter = FilterBuilders.notFilter(FilterBuilders.existsFilter("parentId"));
        }
        else {
            parentFilter = FilterBuilders.termFilter("parentId", parentId);
        }

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
        IndexRequestBuilder idxBuilder = client.prepareIndex(alias, getType())
                .setId(uuidGenerator.generate().toString())
                .setOpType(IndexRequest.OpType.CREATE)
                .setSource(Json.serialize(builder.getDocument()))
                .setRefresh(true);
        String id = idxBuilder.get().getId();
        return get(id);
    }

    @Override
    public boolean update(Folder folder, FolderBuilder builder) {
        // Delete and re-index to delete parentId field when not set
        delete(folder);
        IndexRequestBuilder idxBuilder = client.prepareIndex(alias, getType())
                .setId(folder.getId())
                .setOpType(IndexRequest.OpType.INDEX)
                .setSource(Json.serialize(builder.getDocument()))
                .setRefresh(true);
        String id = idxBuilder.get().getId();
        return id.equals(folder.getId());
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
