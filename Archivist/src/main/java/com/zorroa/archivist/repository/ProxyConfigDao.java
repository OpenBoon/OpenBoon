package com.zorroa.archivist.repository;

import java.util.List;

import com.zorroa.archivist.domain.ProxyConfig;

public interface ProxyConfigDao {

    ProxyConfig get(String id);

    List<ProxyConfig> getAll();

}
