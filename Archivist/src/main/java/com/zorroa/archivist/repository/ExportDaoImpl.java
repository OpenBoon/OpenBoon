package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.util.Json;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;

/**
 * Created by chambers on 11/12/15.
 */
@Repository
public class ExportDaoImpl extends AbstractDao implements ExportDao {

    private static final RowMapper<Export> MAPPER = (rs, row) -> {
        Export export = new Export();
        export.setId(rs.getInt("pk_export"));
        export.setTimeCreated(rs.getLong("time_created"));
        export.setUserCreated(rs.getString("str_user_created"));
        export.setNote(rs.getString("str_note"));
        export.setOptions(Json.deserialize(rs.getString("json_options"), ExportOptions.class));
        export.setSearch(Json.deserialize(rs.getString("json_search"), AssetSearchBuilder.class));
        return export;
    };

    @Override
    public Export get(int id) {
        return jdbc.queryForObject("SELECT * FROM export WHERE pk_export=?", MAPPER, id);
    }

    private static final String INSERT =
            JdbcUtils.insert("export",
                    "str_user_created",
                    "time_created",
                    "str_note",
                    "json_search",
                    "json_options");

    @Override
    public Export create(ExportBuilder builder) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_export"});
            ps.setString(1,SecurityUtils.getUsername());
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, builder.getNote() == null ? "" : builder.getNote());
            ps.setString(4, Json.serializeToString(builder.getSearch()));
            ps.setString(5, Json.serializeToString(builder.getOptions()));
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public boolean setState(Export export, ExportState newState, ExportState oldState) {
        return jdbc.update("UPDATE export SET int_state=? WHERE pk_export=? AND int_state=?",
                newState.ordinal(), export.getId(), oldState.ordinal()) == 1;
    }
}
