package com.zorroa.common.repository;

import com.zorroa.common.config.ApplicationProperties;

import java.util.Map;

/**
 * Created by chambers on 7/8/16.
 */
public interface ClusterSettingsDao {

    String DELIMITER = "_%_";

    Map<String, Object> get();

    void load();

    void save(ApplicationProperties properties);
}
