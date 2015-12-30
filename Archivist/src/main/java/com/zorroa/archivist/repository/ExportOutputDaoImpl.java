package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.ArchivistException;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.domain.ExportState;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.security.SecurityUtils;
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
        output.setUserCreated(rs.getInt("user_created"));
        output.setTimeCreated(rs.getLong("time_created"));
        output.setPath(rs.getString("str_output_file_path"));
        output.setMimeType(rs.getString("str_mime_type"));
        output.setFileExtention(rs.getString("str_file_ext"));
        output.setFileSize(rs.getLong("int_file_size"));
        output.setOffline(rs.getBoolean("bool_offline"));
        output.setTimeOffline(rs.getLong("time_offline"));
        output.setTimeOnline(rs.getLong("time_online"));
        output.setFactory(Json.deserialize(rs.getString("json_factory"),
                new TypeReference<ProcessorFactory<ExportProcessor>>(){}));
        return output;
    };

    @Override
    public ExportOutput get(int id) {
        return jdbc.queryForObject("SELECT * FROM export_output WHERE pk_export_output=?", MAPPER, id);
    }

    @Override
    public void setFileSize(ExportOutput output, long size) {
        jdbc.update("UPDATE export_output SET int_file_size=? WHERE pk_export_output=?", size, output.getId());
    }

    @Override
    public List<ExportOutput> getAll(Export export) {
        return jdbc.query("SELECT * FROM export_output WHERE pk_export=?", MAPPER, export.getId());
    }

    private static final String GET_EXPIRED =
        "SELECT " +
            "export_output.* " +
        "FROM " +
            "export_output, " +
            "export " +
        "WHERE " +
            "export_output.pk_export = export.pk_export " +
        "AND " +
            "export_output.bool_offline = 0 " +
        "AND " +
            "export.int_state = ? " +
        "AND " +
            "(? - export_output.time_online) > ?";

    @Override
    public List<ExportOutput> getAllExpired(long expireTimeMills) {
        return jdbc.query(GET_EXPIRED, MAPPER, ExportState.Finished.ordinal(),
                System.currentTimeMillis(), expireTimeMills);
    }

    @Override
    public boolean setOffline(ExportOutput output) {
        return jdbc.update("UPDATE export_output " +
                "SET bool_offline=1, time_offline=? WHERE pk_export_output=? AND bool_offline=0",
                System.currentTimeMillis(), output.getId()) == 1;
    }

    @Override
    public boolean setOnline(ExportOutput output) {
        return jdbc.update("UPDATE export_output " +
                "SET bool_offline=0, time_online=? WHERE pk_export_output=? AND bool_offline=1",
                System.currentTimeMillis(), output.getId()) == 1;
    }

    private static final String INSERT =
            JdbcUtils.insert("export_output",
                    "pk_export",
                    "user_created",
                    "time_created",
                    "str_mime_type",
                    "str_file_ext",
                    "json_factory");

    @Override
    public ExportOutput create(Export export, ProcessorFactory<ExportProcessor> factory) {

        /**
         * Test creating the new instance.  If this throws the export will
         * not be added.
         */
        ExportProcessor processor = factory.newInstance();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_export_output"});
            ps.setInt(1, export.getId());
            ps.setInt(2, SecurityUtils.getUser().getId());
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, processor.getMimeType());
            ps.setString(5, processor.getFileExtension());
            ps.setString(6, Json.serializeToString(factory));
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        ExportOutput result =  get(id);
        updateOutputPath(export, result);
        return result;
    }

    @Override
    public void updateOutputPath(Export export, ExportOutput output) {
        output.setPath(getOutputFile(export, output));
        jdbc.update("UPDATE export_output SET str_output_file_path=? WHERE pk_export_output=?",
                output.getPath(), output.getId());
    }

    /**
     * Generates a path to store the exported file.  If the path already exists then it
     * avoids overwriting the file by versioning up the path.  Ovrerwriting
     * existing paths may cause issues.
     *
     * @param export
     * @param output
     * @return
     */
    private String getOutputFile(Export export, ExportOutput output) {

        Calendar now = Calendar.getInstance();

        int version = 1;
        File result;

        while(true) {

            String filename = String.format("export%d_output%d",
                    output.getId(), output.getId());

            String path = String.format("%s/%d%d%d/%d/v%d/%s.%s", basePath,
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DATE), export.getId(), version, filename,
                    output.getFileExtention());

            result = new File(path);
            if (result.exists()) {
                version++;
            }
            else {
                break;
            }
        }

        if (result == null) {
            throw new ArchivistException(
                    "Unable to determine file path for export output, ID " + output.getId());
        }

        return result.getAbsolutePath();
    }
}
