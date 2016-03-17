package com.zorroa.archivist.repository;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.Note;
import com.zorroa.archivist.domain.NoteBuilder;
import com.zorroa.archivist.domain.NoteSearch;
import com.zorroa.archivist.sdk.util.Json;
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
import java.util.Map;

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
        Map<String, Object> source = ImmutableMap.<String, Object>builder()
                .put("tags", note.getTags() == null ? new String[] {} : note.getTags())
                .put("text", note.getText())
                .put("assets", note.getAssets())
                .put("createdTime", date)
                .put("modifiedTime", date)
                .put("author", SecurityUtils.getUser().getFirstName() + " " +  SecurityUtils.getUser().getLastName())
                .put("email", SecurityUtils.getUser().getEmail())
                .build();

        IndexResponse rsp = client.prepareIndex(alias, getType(), timeBasedGenerator.generate().toString())
                .setSource(source)
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
                .setQuery(QueryBuilders.termQuery("assets", assetId))
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
