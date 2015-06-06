package com.zorroa.archivist.repository;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.Preconditions;
import org.springframework.stereotype.Repository;

import com.zorroa.archivist.Json;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;

@Repository
public class RoomDaoImpl extends AbstractElasticDao implements RoomDao {

    @Override
    public String getType() {
        return "room";
    }

    private static final JsonRowMapper<Room> MAPPER = new JsonRowMapper<Room>() {
        @Override
        public Room mapRow(String id, long version, byte[] source) {
            Room result = Json.deserialize(source, Room.class);
            result.setId(id);
            result.setVersion(version);
            return result;
        }
    };

    @Override
    public Room create(RoomBuilder builder) {
        Preconditions.checkNotNull(builder.getName(), "The room name cannot be null");
        if (builder.getPassword()!=null) {
            builder.setPassword(SecurityUtils.createPasswordHash(builder.getPassword()));
        }

        String json = new String(Json.serialize(builder));
        IndexResponse resposne = client.prepareIndex(alias, getType())
                   .setSource(json)
                   .setRefresh(true)
                   .get();
        return get(resposne.getId());
    }

    @Override
    public Room get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public String getPassword(String id) {
        return (String) client.prepareGet(alias, getType(), id)
                .setFields("password")
                .get()
                .getField("password")
                .getValue();
    }


}
