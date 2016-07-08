package com.zorroa.common.repository;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import com.zorroa.sdk.plugins.Module;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.util.List;

/**
 * Created by chambers on 6/29/16.
 */
public class ModuleDaoImpl extends AbstractElasticDao implements ModuleDao {

    @Override
    public String getType() {
        return "module";
    }

    private static final JsonRowMapper<Module> MAPPER =
            (id, version, source) -> Json.deserialize(source, Module.class);

    private static final int MAX = 1000;

    @Override
    public List<Module> getAll(String plugin, String type) {

        BoolQueryBuilder q = QueryBuilders.boolQuery();
        if (plugin != null) {
            q.must(QueryBuilders.termQuery("plugin", plugin));
        }
        if (type != null) {
            q.must(QueryBuilders.termQuery("type", type));
        }

        return elastic.query(client.prepareSearch(alias)
                .setTypes(getType())
                .setFrom(0)
                .setSize(MAX)
                .addSort("className", SortOrder.ASC)
                .setQuery(q), MAPPER);
    }

    @Override
    public List<Module> getAll(String plugin) {
        return getAll(plugin, null);
    }

    @Override
    public List<Module> getAll() {
        return elastic.query(client.prepareSearch(alias)
                .setTypes(getType())
                .setFrom(0)
                .setSize(MAX)
                .addSort("className", SortOrder.ASC)
                .setQuery(QueryBuilders.matchAllQuery()), MAPPER);
    }

    @Override
    public PagedList<List<Module>> getPaged(String plugin, Paging paging) {
        return elastic.page(client.prepareSearch(alias)
                .setTypes(getType())
                .addSort("className", SortOrder.ASC)
                .setQuery(QueryBuilders.termQuery("plugin", plugin)), paging, MAPPER);
    }

    @Override
    public Module get(String plugin, String name) {
        return elastic.queryForObject(plugin.concat(":").concat(name), MAPPER);
    }
}
