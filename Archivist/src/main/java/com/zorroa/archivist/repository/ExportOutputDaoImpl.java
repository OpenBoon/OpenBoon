package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.util.Json;
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
public class ExportOutputDaoImpl extends AbstractDao implements ExportOutputDao {

    private static final RowMapper<ExportOutput> MAPPER = (rs, row) -> {
        ExportOutput output = new ExportOutput();
        output.setId(rs.getInt("pk_export_output"));
        output.setExportId(rs.getInt("pk_export"));
        output.setCreatedBy(rs.getString("str_user_created"));
        output.setCreatedTime(rs.getLong("time_created"));
        output.setFactory(Json.deserialize(rs.getString("json_factory"),
                new TypeReference<ProcessorFactory<ExportProcessor>>(){}));
        return output;
    };

    @Override
    public ExportOutput get(int id) {
        return jdbc.queryForObject("SELECT * FROM export_output WHERE pk_export_output=?", MAPPER, id);
    }

    @Override
    public List<ExportOutput> getAll(Export export) {
        return jdbc.query("SELECT * FROM export_output WHERE pk_export=?", MAPPER, export.getId());
    }

    private static final String INSERT =
            JdbcUtils.insert("export_output",
                    "pk_export",
                    "str_user_created",
                    "time_created",
                    "json_factory");

    @Override
    public ExportOutput create(Export export,  ProcessorFactory<ExportProcessor> output) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_export_output"});
            ps.setInt(1, export.getId());
            ps.setString(2, SecurityUtils.getUsername());
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, Json.serializeToString(output));
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }
}
