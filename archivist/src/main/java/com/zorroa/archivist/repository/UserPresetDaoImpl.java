package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.UserPreset;
import com.zorroa.archivist.domain.UserPresetSpec;
import com.zorroa.archivist.domain.UserSettings;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.util.Json;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 10/17/16.
 */
@Repository
public class UserPresetDaoImpl extends AbstractDao implements UserPresetDao {

    private final RowMapper<UserPreset> MAPPER = (rs, row) -> {
        UserPreset p = new UserPreset();
        p.setPresetId(rs.getInt("pk_preset"));
        p.setName(rs.getString("str_name"));
        p.setPermissionIds(getPermissons(rs.getInt("pk_preset")));
        p.setSettings(Json.deserialize(rs.getString("json_settings"), UserSettings.class));
        return p;
    };

    private List<Integer> getPermissons(int id) {
        return jdbc.queryForList("SELECT pk_permission FROM preset_permission  WHERE pk_preset=?",
                Integer.class, id);
    }

    private void setPermissions(int presetId, List<Integer> permissionIds) {
        jdbc.update("DELETE FROM preset_permission WHERE pk_preset=?", presetId);
        if (permissionIds == null) {
            return;
        }

        for (Integer permId: permissionIds) {
            jdbc.update("INSERT INTO preset_permission (pk_preset, pk_permission) VALUES (?,?)",
                    presetId, permId);
        }
    }

    /**
     *
     * @param name
     * @return
     */
    @Override
    public UserPreset get(String name) {
        return jdbc.queryForObject(GET.concat("WHERE str_name=?"), MAPPER, name);
    }

    @Override
    public boolean exists(String name) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM preset WHERE str_name=?", Integer.class, name) > 0;
    }

    private static final String INSERT =
            JdbcUtils.insert("preset",
                    "str_name",
                    "json_settings");
    @Override
    public UserPreset create(UserPresetSpec spec) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_preset"});
            ps.setString(1, spec.getName());
            ps.setString(2, Json.serializeToString(spec.getSettings(), "{}"));
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        setPermissions(id, spec.getPermissionIds());

        return get(id);
    }

    private static final String GET =
            "SELECT " +
                "pk_preset,"+
                "str_name,"+
                "json_settings " +
            "FROM " +
                "preset ";
    @Override
    public UserPreset get(int id) {
        return jdbc.queryForObject(GET.concat("WHERE pk_preset=?"), MAPPER, id);
    }

    @Override
    public UserPreset refresh(UserPreset object) {
        return get(object.getPresetId());
    }

    @Override
    public List<UserPreset> getAll() {
        return jdbc.query("SELECT * FROM preset", MAPPER);
    }

    @Override
    public PagedList<UserPreset> getAll(Pager page) {
        return new PagedList<>(page.setTotalCount(count()),
        jdbc.query(GET.concat("ORDER BY preset.str_name LIMIT ? OFFSET ?"), MAPPER,
                        page.getSize(), page.getFrom()));
    }

    private static final String UPDATE =
        "UPDATE " +
            "preset " +
        "SET " +
            "str_name=?,"+
            "json_settings=? " +
        "WHERE "+
            "pk_preset=?";

    @Override
    public boolean update(int id, UserPreset spec) {
        if (jdbc.update(UPDATE, spec.getName(), Json.serializeToString(spec.getSettings(), "{}"), id)> 0) {
            setPermissions(id, spec.getPermissionIds());
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(int id) {
        return jdbc.update("DELETE FROM preset WHERE pk_preset=?", id) == 1;
    }

    @Override
    public long count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM preset", Long.class);
    }
}
