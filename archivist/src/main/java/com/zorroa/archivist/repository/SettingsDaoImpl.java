package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.zorroa.common.config.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Created by chambers on 5/30/17.
 */
@Repository
public class SettingsDaoImpl extends AbstractDao implements SettingsDao {

    private static final RowMapper<Map<String, String>> MAPPER = (rs, row) ->
            ImmutableMap.of("key", rs.getString("str_name"),
                    "value", rs.getString("str_value"));

    @Override
    public boolean set(String key, Object value) {
        if (isDbVendor("postgresql")) {
            return jdbc.update(
                    "INSERT INTO settings (str_name, str_value) VALUES (?,?) ON CONFLICT(str_name) DO UPDATE SET str_value = ?",
                    key, String.valueOf(value),  String.valueOf(value)) == 1;
        }
        else {
            return jdbc.update("MERGE INTO settings (str_name, str_value) KEY(str_name) VALUES (?,?)",
                    key, String.valueOf(value)) == 1;
        }
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
