package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Created by chambers on 5/30/17.
 */
@Repository
public class SettingsDaoImpl extends AbstractDao implements SettingsDao {

    private static final RowMapper<Map<String, String>> MAPPER = (rs, row) ->
            ImmutableMap.of("key", rs.getString("str_name"),
                    "value", rs.getString("str_value"));

    private static final String MERGE =
            "MERGE INTO " +
                "settings " +
                    "(str_name, " +
                    "str_value) " +
            "KEY(str_name) " +
            "VALUES (?,?)";


    @Override
    public boolean set(String key, Object value) {
        return jdbc.update(MERGE, key, String.valueOf(value)) == 1;
    }

    @Override
    public boolean unset(String key) {
        return jdbc.update("DELETE FROM settings WHERE str_name=?", key) == 1;
    }

    @Override
    public String get(String name) {
        return jdbc.queryForObject("SELECT str_value FROM settings WHERE str_name=?", String.class, name);
    }

    private static final String GET_ALL =
            "SELECT " +
                    "* " +
                    "FROM " +
                    "settings ";
    @Override
    public Map<String, String> getAll() {
        Map<String, String> result = Maps.newHashMap();
        jdbc.query(GET_ALL, rs -> {
            result.put(rs.getString("str_name"),
                    rs.getString("str_value"));
        });
        return result;
    }
}
