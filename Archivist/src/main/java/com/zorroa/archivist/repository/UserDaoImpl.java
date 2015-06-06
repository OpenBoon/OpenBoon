package com.zorroa.archivist.repository;

import org.elasticsearch.common.Preconditions;
import org.springframework.stereotype.Repository;

import com.zorroa.archivist.Json;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserBuilder;

@Repository
public class UserDaoImpl extends AbstractElasticDao implements UserDao {

    @Override
    public String getType() {
        return "user";
    }

    private static final JsonRowMapper<User> MAPPER = new JsonRowMapper<User>() {
        @Override
        public User mapRow(String id, long version, byte[] source) {
            User result = Json.deserialize(source, User.class);
            result.setId(id);
            result.setVersion(version);
            return result;
        }
    };

    @Override
    public User get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public User create(UserBuilder builder) {
        Preconditions.checkNotNull(builder.getUserId(), "The Username cannot be null");
        Preconditions.checkNotNull(builder.getPassword(), "The Password cannot be null");

        builder.setPassword(SecurityUtils.createPasswordHash(builder.getPassword()));
        String json = new String(Json.serialize(builder));
        client.prepareIndex(alias, getType(), builder.getUserId())
                   .setSource(json)
                   .setRefresh(true)
                   .get();
        return get(builder.getUserId());
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
