package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.util.Json;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.List;

/**
 * Created by chambers on 11/12/15.
 */
@Repository
public class ExportOutputDaoImpl extends AbstractDao implements ExportOutputDao {

    @Value("${archivist.export.basePath}")
    private String basePath;

    private static final RowMapper<ExportOutput> MAPPER = (rs, row) -> {
        ExportOutput output = new ExportOutput();
        output.setId(rs.getInt("pk_export_output"));
        output.setExportId(rs.getInt("pk_export"));
        output.setCreatedBy(rs.getString("str_user_created"));
        output.setCreatedTime(rs.getLong("time_created"));
        output.setPath(rs.getString("str_output_file_path"));
        output.setMimeType(rs.getString("str_mime_type"));
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
                    "str_mime_type",
                    "json_factory");

    @Override
    public ExportOutput create(Export export, ProcessorFactory<ExportProcessor> factory) {

        ExportProcessor processor = factory.newInstance();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_export_output"});
            ps.setInt(1, export.getId());
            ps.setString(2, SecurityUtils.getUsername());
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, processor.getMimeType());
            ps.setString(5, Json.serializeToString(factory));
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        ExportOutput result =  get(id);

        /*
         * Once the output is added, we can generate a file name because we may want to use
         * the output ID in the name.  The filename can be overridden by passing in
         * a "baseFileName" in with the factory args.  baseFileName should not contain the
         * file extension.
         */
        result.setPath(getOutputFile(export, result,
                factory.getArg("baseFileName"), processor.getFileExtension()));

        jdbc.update("UPDATE export_output SET str_output_file_path=? WHERE pk_export_output=?",
                result.getPath(), result.getId());
        return result;
    }

    /**
     * Generates a path to store the exported file.
     *
     * @param export
     * @param output
     * @param filename
     * @param ext
     * @return
     */
    private String getOutputFile(Export export, ExportOutput output, Object filename, String ext) {

        Calendar now = Calendar.getInstance();

        if (filename == null) {
            filename = String.format("zorroaExport%d-%s-%d%d%d",
                    output.getId(), SecurityUtils.getUsername(),
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE));
        }

        String path = String.format("%s/%d%d%d/%d/%s.%s",
                basePath,
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DATE),  export.getId(), filename, ext);
        return new File(path).getAbsolutePath();
    }
}
