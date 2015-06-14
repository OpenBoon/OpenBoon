package com.zorroa.archivist.repository;

import java.util.List;

import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.domain.ProxyConfigBuilder;
import com.zorroa.archivist.domain.ProxyOutput;

public interface ProxyConfigDao {

    ProxyConfig get(String id);

    List<ProxyConfig> getAll();

    ProxyConfig get(int id);

    ProxyConfig create(ProxyConfigBuilder builder);

    List<ProxyOutput> getAllOutputs(int id);

    List<ProxyOutput> getAllOutputs(ProxyConfig config);

}
