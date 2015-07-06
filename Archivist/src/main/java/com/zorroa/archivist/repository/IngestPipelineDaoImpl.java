package com.zorroa.archivist.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;

@Repository
public class IngestPipelineDaoImpl extends AbstractDao implements IngestPipelineDao {

    private static final RowMapper<IngestPipeline> MAPPER = new RowMapper<IngestPipeline>() {
        @Override
        public IngestPipeline mapRow(ResultSet rs, int row) throws SQLException {
            IngestPipeline result = new IngestPipeline();
            result.setId(rs.getInt("pk_pipeline"));
            result.setName(rs.getString("str_name"));
            result.setDescription(rs.getString("str_description"));
            result.setTimeCreated(rs.getLong("time_created"));
            result.setUserCreated(rs.getString("str_user_created"));
            result.setTimeModified(rs.getLong("time_modified"));
            result.setUserModified(rs.getString("str_user_modified"));
            result.setProcessors((List<IngestProcessorFactory>) rs.getObject("list_processors"));
            return result;
        }
    };

    private static final String INSERT =
            "INSERT INTO " +
                    "pipeline " +
            "(" +
                    "str_name,"+
                    "str_description,"+
                    "str_user_created,"+
                    "time_created,"+
                    "str_user_modified, "+
                    "time_modified, "+
                    "list_processors " +
            ") "+
            "VALUES (?,?,?,?,?,?,?)";

    @Override
    public IngestPipeline create(IngestPipelineBuilder builder) {
        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                connection.prepareStatement(INSERT, new String[]{"pk_pipeline"});
            ps.setString(1, builder.getName());
            ps.setString(2, builder.getDescription());
            ps.setString(3, SecurityUtils.getUsername());
            ps.setLong(4, time);
            ps.setString(5, SecurityUtils.getUsername());
            ps.setLong(6, time);
            ps.setObject(7, builder.getProcessors());
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public IngestPipeline get(int id) {
        try {
            return jdbc.queryForObject("SELECT * FROM pipeline WHERE pk_pipeline=?", MAPPER, id);
        } catch(EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException("Failed to get pipeline: id=" + id, 1);
        }
    }

    @Override
    public IngestPipeline get(String name) {
        return jdbc.queryForObject("SELECT * FROM pipeline WHERE str_name=?", MAPPER, name);
    }

    @Override
    public List<IngestPipeline> getAll() {
        return jdbc.query("SELECT * FROM pipeline", MAPPER);
    }
}
