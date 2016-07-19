package com.zorroa.common.repository;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import com.zorroa.sdk.plugins.Plugin;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.util.List;

/**
 * Created by chambers on 6/29/16.
 */
public class PluginDaoImpl extends AbstractElasticDao implements PluginDao {

    @Override
    public String getType() {
        return "plugin";
    }

    private static final JsonRowMapper<Plugin> MAPPER =
            (id, version, source) -> Json.deserialize(source, Plugin.class);

    @Override
    public List<Plugin> getAll() {
        return elastic.query(client.prepareSearch(alias)
                .setTypes(getType())
                .setFrom(0)
                .setSize(1000)
                .addSort("name", SortOrder.ASC)
                .setQuery(QueryBuilders.matchAllQuery()), MAPPER);
    }

    @Override
    public PagedList<Plugin> getAll(Paging paging) {
        return elastic.page(client.prepareSearch(alias)
                .setTypes(getType())
                .setQuery(QueryBuilders.matchAllQuery()), paging, MAPPER);
    }

    @Override
    public Plugin get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }
}
