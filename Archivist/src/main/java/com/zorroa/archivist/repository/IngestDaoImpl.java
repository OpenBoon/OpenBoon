package com.zorroa.archivist.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.elasticsearch.common.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestState;
import com.zorroa.archivist.domain.ProxyConfig;

@Repository
public class IngestDaoImpl extends AbstractDao implements IngestDao {

    private static final RowMapper<Ingest> MAPPER = new RowMapper<Ingest>() {
        @Override
        public Ingest mapRow(ResultSet rs, int row) throws SQLException {
            Ingest result = new Ingest();
            result.setId(rs.getLong("pk_ingest"));
            result.setPipelineId(rs.getInt("pk_pipeline"));
            result.setProxyConfigId(rs.getInt("pk_proxy_config"));
            result.setState(IngestState.values()[rs.getInt("int_state")]);
            result.setPath(rs.getString("str_path"));
            result.setTimeCreated(rs.getLong("time_created"));
            result.setUserCreated(rs.getString("str_user_created"));
            result.setTimeModified(rs.getLong("time_modified"));
            result.setUserCreated(rs.getString("str_user_created"));
            result.setFileTypes(ImmutableSet.copyOf((String[]) rs.getArray("list_types").getArray()));
            result.setTimeStarted(rs.getLong("time_started"));
            result.setTimeStopped(rs.getLong("time_stopped"));
            result.setCreatedCount(rs.getInt("int_created_count"));
            result.setErrorCount(rs.getInt("int_error_count"));
            return result;
        }
    };

    @Override
    public Ingest get(long id) {
        return jdbc.queryForObject("SELECT * FROM ingest WHERE pk_ingest=?", MAPPER, id);
    }

    private static final String INSERT =
            "INSERT INTO " +
                "ingest " +
            "(" +
                "pk_pipeline,"+
                "pk_proxy_config,"+
                "int_state,"+
                "str_path,"+
                "list_types,"+
                "time_created,"+
                "str_user_created,"+
                "time_modified, "+
                "str_user_modified "+

            ") " +
            "VALUES (" + StringUtils.repeat("?", ",", 9) + ")";

    @Override
    public Ingest create(IngestPipeline pipeline, ProxyConfig proxyConfig, IngestBuilder builder) {
        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                connection.prepareStatement(INSERT, new String[]{"pk_pipeline"});
            ps.setInt(1, pipeline.getId());
            ps.setInt(2, proxyConfig.getId());
            ps.setInt(3, IngestState.Waiting.ordinal());
            ps.setObject(4, builder.getPath());
            ps.setObject(5, builder.getFileTypes().toArray(new String[] {}));
            ps.setLong(6, time);
            ps.setString(7, SecurityUtils.getUsername());
            ps.setLong(8, time);
            ps.setString(9, SecurityUtils.getUsername());
            return ps;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return get(id);
    }

    @Override
    public Ingest getNextWaitingIngest() {
        try {
            return jdbc.queryForObject("SELECT * FROM ingest WHERE int_state=? ORDER BY time_created ASC LIMIT 1",
                    MAPPER, IngestState.Waiting.ordinal());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public boolean setRunning(Ingest ingest) {
        return jdbc.update("UPDATE ingest SET int_state=? WHERE pk_ingest=? AND int_state=?",
                IngestState.Running.ordinal(), ingest.getId(), IngestState.Waiting.ordinal()) > 0;
    }


    @Override
    public boolean setFinished(Ingest ingest) {
        return jdbc.update("UPDATE ingest SET int_state=? WHERE pk_ingest=? AND int_state=?",
                IngestState.Finished.ordinal(), ingest.getId(), IngestState.Running.ordinal()) > 0;
    }

    @Override
    public void incrementCreatedCount(Ingest ingest, int increment) {
        logger.info("incrementing created count");
        jdbc.update("UPDATE ingest SET int_created_count=int_created_count+? WHERE pk_ingest=?",
                increment, ingest.getId());
    }

    @Override
    public void incrementErrorCount(Ingest ingest, int increment) {
        jdbc.update("UPDATE ingest SET int_error_count=int_error_count+? WHERE pk_ingest=?",
                increment, ingest.getId());
    }

}
