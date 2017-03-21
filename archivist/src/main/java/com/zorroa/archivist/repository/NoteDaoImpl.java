package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteSpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by chambers on 3/16/16.
 */
@Repository
public class NoteDaoImpl extends AbstractElasticDao implements NoteDao {

    @Autowired
    UserDaoCache userDaoCache;

    @Override
    public Note create(NoteSpec spec) {
        ImmutableMap.Builder<String, Object> map = ImmutableMap.<String, Object>builder()
                .put("text", spec.getText())
                .put("asset", spec.getAsset())
                .put("timeCreated", System.currentTimeMillis())
                .put("userId", SecurityUtils.getUser().getId());

        IndexResponse rsp = client.prepareIndex(getIndex(), getType())
                .setSource(Json.serializeToString(map.build()))
                .setRefresh(true)
                .get();

        return get(rsp.getId());
    }

    private final JsonRowMapper<Note> MAPPER = (id, version, score, source) -> {
        Note note = Json.Mapper.readValue(source, Note.class);
        note.setId(id);
        note.setUser(userDaoCache.getUser(note.getUserId()));
        return note;
    };

    @Override
    public Note get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public List<Note> getAll(String assetId) {
        return elastic.query(client.prepareSearch(getIndex())
                        .setTypes(getType())
                        .setQuery(QueryBuilders.termQuery("asset", assetId))
                        .addSort(SortBuilders.fieldSort("timeCreated"))
                        .setSize(1000),
                MAPPER);
    }

    @Override
    public String getType() {
        return "note";
    }

    @Override
    public String getIndex() { return "notes"; }
}
