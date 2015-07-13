package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.domain.ProxyConfigBuilder;
import com.zorroa.archivist.domain.ProxyConfigUpdateBuilder;
import com.zorroa.archivist.domain.ProxyOutput;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.Preconditions;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ProxyConfigDaoImpl extends AbstractDao implements ProxyConfigDao {

    private static final RowMapper<ProxyConfig> MAPPER = new RowMapper<ProxyConfig>() {
        @Override
        public ProxyConfig mapRow(ResultSet rs, int row) throws SQLException {
            ProxyConfig result = new ProxyConfig();
            result.setId(rs.getInt("pk_proxy_config"));
            result.setName(rs.getString("str_name"));
            result.setDescription(rs.getString("str_description"));
            result.setTimeCreated(rs.getLong("time_created"));
            result.setUserCreated(rs.getString("str_user_created"));
            result.setTimeModified(rs.getLong("time_modified"));
            result.setUserModified(rs.getString("str_user_modified"));
            result.setOutputs((List<ProxyOutput>)rs.getObject("list_outputs"));
            return result;
        }
    };

    private static final RowMapper<ProxyOutput> MAPPER_OUTPUT = new RowMapper<ProxyOutput>() {
        @Override
        public ProxyOutput mapRow(ResultSet rs, int row) throws SQLException {
            ProxyOutput result = new ProxyOutput();
            result.setBpp(rs.getInt("int_bpp"));
            result.setFormat(rs.getString("str_format"));
            result.setSize(rs.getInt("int_size"));
            return result;
        }
    };

    @Override
    public ProxyConfig get(int id) {
        return jdbc.queryForObject("SELECT * FROM proxy_config WHERE pk_proxy_config=?", MAPPER, id);
    }

    @Override
    public ProxyConfig get(String name) {
        return jdbc.queryForObject("SELECT * FROM proxy_config WHERE str_name=?", MAPPER, name);
    }

    @Override
    public List<ProxyConfig> getAll() {
        return jdbc.query("SELECT * FROM proxy_config", MAPPER);
    }

    private static final String INSERT =
            "INSERT INTO " +
                    "proxy_config " +
            "(" +
                    "str_name,"+
                    "str_description,"+
                    "str_user_created,"+
                    "time_created,"+
                    "str_user_modified, "+
                    "time_modified, "+
                    "list_outputs " +
            ") "+
            "VALUES (?,?,?,?,?,?,?)";

    @Override
    public ProxyConfig create(ProxyConfigBuilder builder) {
         Preconditions.checkNotNull(builder.getName(), "The proxy config name cannot be null");
         long time = System.currentTimeMillis();
         KeyHolder keyHolder = new GeneratedKeyHolder();
         jdbc.update(connection -> {
             PreparedStatement ps =
                 connection.prepareStatement(INSERT, new String[]{"pk_proxy_config"});
             ps.setString(1, builder.getName());
             ps.setString(2, builder.getDescription());
             ps.setString(3, SecurityUtils.getUsername());
             ps.setLong(4, time);
             ps.setString(5, SecurityUtils.getUsername());
             ps.setLong(6, time);
             ps.setObject(7, builder.getOutputs());
             return ps;
         }, keyHolder);
         int id = keyHolder.getKey().intValue();
         ProxyConfig config =  get(id);
         return config;
    }

    @Override
    public boolean update(ProxyConfig config, ProxyConfigUpdateBuilder builder) {

        StringBuilder sb = new StringBuilder(512);
        sb.append("UPDATE proxy_config SET ");

        List<String> updates = Lists.newArrayList();
        List<Object> values = Lists.newArrayList();

        if (builder.getDescription() != null) {
            updates.add("str_description=?");
            values.add(builder.getDescription());
        }

        if (builder.getName() != null) {
            updates.add("str_name=?");
            values.add(builder.getName());
        }

        if (builder.getOutputs() != null) {
            updates.add("list_outputs=?");
            values.add(builder.getOutputs());
        }

        if (updates.isEmpty()) {
            return false;
        }

        updates.add("str_user_modified=?");
        values.add(SecurityUtils.getUsername());

        updates.add("time_modified=?");
        values.add(System.currentTimeMillis());

        sb.append(StringUtils.join(updates, ", "));
        sb.append(" WHERE pk_proxy_config=?");
        values.add(config.getId());

        logger.debug("{} {}", sb.toString(), values);
        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    @Override
    public boolean delete(ProxyConfig config) {
        return jdbc.update("DELETE FROM proxy_config WHERE pk_proxy_config=?", config.getId()) == 1;
    }
}
