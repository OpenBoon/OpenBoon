package com.zorroa.archivist.sdk.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by chambers on 2/17/16.
 */
public interface ApplicationProperties {
    <T> T get(String key, Class<T> type);
    <T> T get(String key, T def);
    String getString(String key);
    String getString(String key, String def);
    Iterable<String> split(String key, String delimiter);
    Path getPath(String key);
    Path getPath(String key, Path def);
    int getInt(String key);
    double getDouble(String key);
    boolean getBoolean(String key);
    int getInt(String key, int def);
    double getDouble(String key, double def);
    boolean getBoolean(String key, boolean def);
    Map<String,Object> getMap(String prefix);
    int max(String key, int value);
    double max(String key, double value);
    int min(String key, int value);
    double min(String key, double value);
    Properties getProperties(String prefix);
    Properties getProperties(String prefix, boolean includePrefix);
    List<String> getList(String key);
}
