package com.zorroa.archivist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.domain.Setting;
import com.zorroa.archivist.domain.SettingsFilter;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 5/30/17.
 */
public interface SettingsService {

    TypeReference< List<Setting>>
            LIST_OF_SETTINGS = new TypeReference< List<Setting>>() {};

    int setAll(Map<String, Object> values);

    boolean set(String key, String value);

    List<Setting> getAll(SettingsFilter filter);

    List<Setting> getAll();

    Setting get(String name);

    void checkValid(String key, String value);
}
