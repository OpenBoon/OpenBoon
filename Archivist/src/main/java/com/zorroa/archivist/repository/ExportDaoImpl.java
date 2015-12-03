package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 11/12/15.
 */
@Repository
public class ExportDaoImpl extends AbstractDao implements ExportDao {

    private static final RowMapper<Export> MAPPER = (rs, row) -> {
        Export export = new Export();
        export.setId(rs.getInt("pk_export"));
        export.setState(ExportState.values()[rs.getInt("int_state")]);
        export.setTimeCreated(rs.getLong("time_created"));
        export.setUserCreated(rs.getInt("user_created"));
        export.setNote(rs.getString("str_note"));
        export.setOptions(Json.deserialize(rs.getString("json_options"), ExportOptions.class));
        export.setSearch(Json.deserialize(rs.getString("json_search"), AssetSearch.class));
        export.setTotalFileSize(rs.getLong("int_total_file_size"));
        export.setAssetCount(rs.getInt("int_asset_count"));
        return export;
    };

    @Override
    public Export get(int id) {
        return jdbc.queryForObject("SELECT * FROM export WHERE pk_export=?", MAPPER, id);
    }

    @Override
    public List<Export> getAll(ExportState state, int limit) {
        Preconditions.checkArgument(limit > 0, "Limit must be greater than 0");
        return jdbc.query("SELECT * FROM export WHERE int_state=? ORDER BY time_created ASC LIMIT ?",
                MAPPER, state.ordinal(), limit);
    }

    @Override
    public List<Export> getAll(ExportFilter filter) {

        StringBuilder sb = new StringBuilder(512);
        sb.append("SELECT * FROM export");

        List<String> wheres = Lists.newArrayList();
        List<Object> values = Lists.newArrayList();

        if (filter.getStates() != null) {
            wheres.add(JdbcUtils.in("int_state", filter.getStates().size()));
            filter.getStates().forEach(value -> values.add(value.ordinal()));
        }

        if (filter.getUsers() != null) {
            wheres.add(JdbcUtils.in("export.user_created", filter.getUsers().size()));
            values.addAll(filter.getUsers());
        }

        if (filter.getAfterTime() > 0) {
            wheres.add("time_created > ?");
            values.add(filter.getAfterTime());
        }

        if (filter.getBeforeTime() > 0) {
            wheres.add("time_created < ?");
            values.add(filter.getBeforeTime());
        }

        if (!wheres.isEmpty()) {
            sb.append(" WHERE ");
            sb.append(StringUtils.join(wheres, " AND "));
        }

        // newest first
        sb.append(" ORDER BY time_created DESC ");
        sb.append(String.format("LIMIT %d OFFSET %d",
                filter.getPageSize(), (filter.getPage() - 1) *  filter.getPageSize()));

        return jdbc.query(sb.toString(), MAPPER, values.toArray());
    }

    private static final String INSERT =
            JdbcUtils.insert("export",
                    "user_created",
                    "time_created",
                    "str_note",
                    "json_search",
                    "json_options",
                    "int_total_file_size",
                    "int_asset_count");

    @Override
    public Export create(ExportBuilder builder, long totalFileSize, long assetCount) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_export"});
            ps.setInt(1,SecurityUtils.getUser().getId());
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, builder.getNote() == null ? "" : builder.getNote());
            ps.setString(4, Json.serializeToString(builder.getSearch()));
            ps.setString(5, Json.serializeToString(builder.getOptions()));
            ps.setLong(6, totalFileSize);
            ps.setLong(7, assetCount);
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    private static final String QUEUED =
            "UPDATE " +
                "export " +
            "SET " +
                "time_started=-1," +
                "time_stopped=-1," +
                "int_state=? "+
            "WHERE " +
                "pk_export=? "+
            "AND " +
                "int_state=?";
    @Override
    public boolean setQueued(Export export) {
        return jdbc.update(QUEUED,
                ExportState.Queued.ordinal(),
                export.getId(),
                ExportState.Finished.ordinal()) == 1;
    }

    private static final String RUNNING =
            "UPDATE " +
                "export " +
            "SET " +
                "time_started=?," +
                "int_state=?,"+
                "time_stopped=-1,"+
                "int_execute_count=int_execute_count+1 " +
            "WHERE " +
                "pk_export=? "+
            "AND " +
                "int_state=?";
    @Override
    public boolean setRunning(Export export) {
        return jdbc.update(RUNNING,
                System.currentTimeMillis(),
                ExportState.Running.ordinal(),
                export.getId(),
                ExportState.Queued.ordinal()) == 1;
    }

    private static final String FINISHED =
            "UPDATE " +
                "export " +
            "SET " +
                "time_stopped=?,"+
                "int_state=? "+
            "WHERE " +
                "pk_export=? "+
            "AND " +
                "int_state=?";

    @Override
    public boolean setFinished(Export export) {
        return jdbc.update(FINISHED,
                System.currentTimeMillis(),
                ExportState.Finished.ordinal(),
                export.getId(),
                ExportState.Running.ordinal()) == 1;
    }

    @Override
    public boolean setState(Export export, ExportState newState, ExportState oldState) {
        return jdbc.update("UPDATE export SET int_state=? WHERE pk_export=? AND int_state=?",
                newState.ordinal(), export.getId(), oldState.ordinal()) == 1;
    }

    @Override
    public boolean setSearch(Export export, AssetSearch search) {
        return jdbc.update("UPDATE export SET json_search=? WHERE pk_export=? AND int_state=?",
            Json.serializeToString(search), export.getId(), ExportState.Queued.ordinal()) == 1;
    }
}
