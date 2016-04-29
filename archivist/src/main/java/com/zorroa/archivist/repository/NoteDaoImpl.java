package com.zorroa.archivist.repository;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.google.common.collect.ImmutableMap;
import com.zorroa.sdk.domain.Note;
import com.zorroa.sdk.domain.NoteBuilder;
import com.zorroa.sdk.domain.NoteSearch;
import com.zorroa.sdk.util.Json;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Created by chambers on 3/16/16.
 */
@Repository
public class NoteDaoImpl extends AbstractElasticDao implements NoteDao {

    private final TimeBasedGenerator timeBasedGenerator =
            Generators.timeBasedGenerator(EthernetAddress.fromInterface());

    @Override
    public Note create(NoteBuilder note) {
        Date date = new Date();
        ImmutableMap.Builder<String, Object> map = ImmutableMap.<String, Object>builder()
                .put("text", note.getText())
                .put("asset", note.getAsset())
                .put("createdTime", date)
                .put("modifiedTime", date)
                .put("author", SecurityUtils.getUser().getFirstName() + " " +  SecurityUtils.getUser().getLastName())
                .put("email", SecurityUtils.getUser().getEmail());

        if (note.getAnnotations() != null) {
            map.put("annotations", note.getAnnotations());
        }

        if (note.getTags() != null) {
            map.put("tags", note.getTags());
        }

        IndexResponse rsp = client.prepareIndex(alias, getType(), timeBasedGenerator.generate().toString())
                .setSource(Json.serializeToString(map.build()))
                .setRefresh(true)
                .get();

        return get(rsp.getId());
    }

    private static final JsonRowMapper<Note> MAPPER = (id, version, source) -> {
        Note note = Json.Mapper.readValue(source, Note.class);
        note.setId(id);
        return note;
    };

    @Override
    public Note get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public List<Note> getAll(String assetId) {
        return elastic.query(client.prepareSearch(alias)
                        .setTypes(getType())
                        .setQuery(QueryBuilders.termQuery("asset", assetId))
                        .addSort(SortBuilders.fieldSort("createdTime"))
                        .setSize(1000),
                MAPPER);
    }

    @Override
    public List<Note> search(NoteSearch search) {
        SearchRequestBuilder req = client.prepareSearch(alias)
                .setTypes(getType())
                .setQuery(QueryBuilders.matchQuery("_all", search.getQuery()))
                .setSize(search.getSize())
                .setFrom((search.getPage() -1) * search.getSize());
        return elastic.query(req, MAPPER);
    }

    @Override
    public String getType() {
        return "note";
    }
}
