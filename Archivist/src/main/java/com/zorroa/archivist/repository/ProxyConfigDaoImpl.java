package com.zorroa.archivist.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.ProxyConfig;

@Repository
public class ProxyConfigDaoImpl extends AbstractElasticDao implements ProxyConfigDao {

    @Override
    public String getType() {
        return "proxy-config";
    }

    private static final JsonRowMapper<ProxyConfig> MAPPER = new JsonRowMapper<ProxyConfig>() {
        @Override
        public ProxyConfig mapRow(String id, long version, byte[] source) {
            ProxyConfig result = Json.deserialize(source, ProxyConfig.class);
            result.setId(id);
            result.setVersion(version);
            return result;
        }
    };

    @Override
    public ProxyConfig get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public List<ProxyConfig> getAll() {
        return elastic.query(MAPPER);
    }
}
