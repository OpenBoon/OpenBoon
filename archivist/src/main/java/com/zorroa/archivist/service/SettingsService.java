package com.zorroa.archivist.service;

import java.util.Map;

/**
 * Created by chambers on 5/30/17.
 */
public interface SettingsService {
    void setAll(Map<String, Object> values);

    Map<String,String> getAll();

    boolean isValid(String key, Object value);
}
