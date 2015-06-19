package com.zorroa.archivist.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.elasticsearch.common.lang3.StringUtils;
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

@Repository
public class IngestDaoImpl extends AbstractDao implements IngestDao {

    private static final RowMapper<Ingest> MAPPER = new RowMapper<Ingest>() {
        @Override
        public Ingest mapRow(ResultSet rs, int row) throws SQLException {
            Ingest result = new Ingest();
            result.setId(rs.getLong("pk_ingest"));
            result.setPipelineId(rs.getInt("pk_pipeline"));
            result.setState(IngestState.values()[rs.getInt("int_state")]);
            result.setPath(rs.getString("str_path"));
            result.setTimeCreated(rs.getLong("time_created"));
            result.setUserCreated(rs.getString("str_user_created"));
            result.setTimeModified(rs.getLong("time_modified"));
            result.setUserCreated(rs.getString("str_user_created"));
            result.setTypes(ImmutableSet.copyOf((String[]) rs.getArray("list_types").getArray()));
            result.setTimeStarted(rs.getLong("time_started"));
            result.setTimeStopped(rs.getLong("time_stopped"));
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
                "int_state,"+
                "str_path,"+
                "list_types,"+
                "time_created,"+
                "str_user_created,"+
                "time_modified, "+
                "str_user_modified "+

            ") " +
            "VALUES (" + StringUtils.repeat("?", ",", 8) + ")";

    @Override
    public Ingest create(IngestPipeline pipeline, IngestBuilder builder) {
        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                connection.prepareStatement(INSERT, new String[]{"pk_pipeline"});
            ps.setInt(1, pipeline.getId());
            ps.setInt(2, IngestState.Waiting.ordinal());
            ps.setObject(3, builder.getPath());
            ps.setObject(4, builder.getFileTypes().toArray(new String[] {}));
            ps.setLong(5, time);
            ps.setString(6, SecurityUtils.getUsername());
            ps.setLong(7, time);
            ps.setString(8, SecurityUtils.getUsername());
            return ps;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return get(id);

    }

}
