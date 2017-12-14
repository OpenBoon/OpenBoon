package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.ExportFile;
import com.zorroa.archivist.domain.ExportFileSpec;
import com.zorroa.archivist.domain.Job;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class ExportDaoImpl extends AbstractDao implements ExportDao {

    private static final String INSERT =
            JdbcUtils.insert("export_file",
                    "pk_job",
                    "str_name",
                    "str_path",
                    "str_mime_type",
                    "int_size",
                    "time_created");

    @Override
    public ExportFile createExportFile(Job job, ExportFileSpec spec) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement ps =
                        connection.prepareStatement(INSERT, new String[]{"pk_export_file"});
                ps.setLong(1, job.getJobId());
                ps.setString(2, spec.getName());
                ps.setString(3, job.getRootPath() + "/exported/" + spec.getName());
                ps.setString(4, spec.getMimeType());
                ps.setLong(5, spec.getSize());
                ps.setLong(6, System.currentTimeMillis());
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("The export file " + spec.getName()
                    + " in job " + job.getJobId() + " already exists");
        }

        int id = keyHolder.getKey().intValue();
        return getExportFile(id);

    }

    private static final RowMapper<ExportFile> MAPPER_EXPORT_FILE = (rs, row) -> {
        ExportFile file = new ExportFile();
        file.setId(rs.getLong("pk_export_file"));
        file.setJobId(rs.getLong("pk_job"));
        file.setMimeType(rs.getString("str_mime_type"));
        file.setName(rs.getString("str_name"));
        file.setSize(rs.getLong("int_size"));
        file.setTimeCreated(rs.getLong("time_created"));
        return file;
    };

    private static final String GET =
            "SELECT " +
                "pk_export_file,"+
                "pk_job,"+
                "str_mime_type,"+
                "str_name,"+
                "str_path,"+
                "int_size,"+
                "time_created " +
            "FROM " +
                "export_file ";

    @Override
    public ExportFile getExportFile(long id) {
        return jdbc.queryForObject(GET.concat(" WHERE pk_export_file=?"), MAPPER_EXPORT_FILE, id);
    }

    @Override
    public List<ExportFile> getAllExportFiles(Job job) {
        return jdbc.query(GET.concat(" WHERE pk_job=?"), MAPPER_EXPORT_FILE, job.getJobId());
    }
}
