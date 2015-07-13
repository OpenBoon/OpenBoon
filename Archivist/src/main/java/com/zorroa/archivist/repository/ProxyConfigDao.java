package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.domain.ProxyConfigBuilder;
import com.zorroa.archivist.domain.ProxyConfigUpdateBuilder;

import java.util.List;

public interface ProxyConfigDao {

    ProxyConfig get(String id);

    List<ProxyConfig> getAll();

    ProxyConfig get(int id);

    ProxyConfig create(ProxyConfigBuilder builder);

    boolean update(ProxyConfig config, ProxyConfigUpdateBuilder builder);

    boolean delete(ProxyConfig config);
}
