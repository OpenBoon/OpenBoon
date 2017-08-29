package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.SharedLink;
import com.zorroa.archivist.domain.SharedLinkSpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.util.Json;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;

/**
 */
@Repository
public class SharedLinkDaoImpl extends AbstractDao implements SharedLinkDao {

    private final RowMapper<SharedLink> MAPPER = (rs, row) -> {
        SharedLink link = new SharedLink();
        link.setId(rs.getInt("pk_shared_link"));
        link.setState(Json.deserialize(rs.getString("json_state"),
                Json.GENERIC_MAP));
        link.setExpireTime(rs.getLong("time_expired"));
        return link;
    };

    private static final String INSERT =
            JdbcUtils.insert("shared_link",
                    "pk_user",
                    "time_created",
                    "time_expired",
                    "json_state",
                    "json_users");

    @Override
    public SharedLink create(SharedLinkSpec spec) {

        long defaultExpireTimeMs = TimeUnit.HOURS.toMillis(
                properties.getInt("archivist.maintenance.sharedLinks.expireDays"));

        long expireTime;
        if (spec.getExpireTimeMs() != null) {
            expireTime = System.currentTimeMillis() + spec.getExpireTimeMs();
        }
        else {
            expireTime = System.currentTimeMillis() + defaultExpireTimeMs;
        }

        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_shared_link"});
            ps.setInt(1, SecurityUtils.getUser().getId());
            ps.setLong(2, System.currentTimeMillis());
            ps.setLong(3, expireTime);
            ps.setString(4, Json.serializeToString(spec.getState(), "{}"));
            ps.setString(5, Json.serializeToString(spec.getUserIds(), "[]"));
            return ps;
        }, key);

        int id = key.getKey().intValue();
        return get(id);
    }

    @Override
    public SharedLink get(int id) {
        return jdbc.queryForObject("SELECT * FROM shared_link WHERE pk_shared_link=?",
                MAPPER, id);
    }

    @Override
    public int deleteExpired(long olderThan) {
        return jdbc.update("DELETE FROM shared_link WHERE time_expired < ?", olderThan);
    }
}
