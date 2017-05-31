package com.zorroa.archivist.repository;

import java.util.Map;

/**
 * Created by chambers on 5/30/17.
 */
public interface SettingsDao {

    void set(String key, Object value);

    boolean unset(String key);

    String get(String name);

    Map<String, String> getAll();
}
