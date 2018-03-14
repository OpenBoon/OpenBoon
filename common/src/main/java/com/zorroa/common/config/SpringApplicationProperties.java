package com.zorroa.common.config;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.config.ApplicationProperties;
import com.zorroa.archivist.sdk.config.ApplicationPropertiesException;
import com.zorroa.sdk.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by chambers on 2/12/16.
 */
public class SpringApplicationProperties implements ApplicationProperties {

    @Autowired
    ConfigurableEnvironment env;

    public SpringApplicationProperties() { }

    public <T> T get(String key, Class<T> type) { return env.getProperty(key, type); }

    public <T> T get(String key, T def) {
        T val = (T) env.getProperty(key, def.getClass());
        if (val == null) {
            return def;
        }
        return val;
    }

    public String getString(String key) {
        String result =  env.getProperty(key);
        if (result == null) {
            throw new ApplicationPropertiesException("Configuration key not found: '" + key + "'");
        }
        return result.trim();
    }

    public String getString(String key, String def) {
        String result =  env.getProperty(key);
        if (result == null) {
            return def;
        }
        return result.trim();
    }

    @Override
    public List<String> getList(String key) {
        String value = getString(key);
        if (value.startsWith("file:")) {
            try {
                List<String> result = Lists.newArrayList();
                String path = value.split(":")[1];
                try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) {
                            continue;
                        }
                        if (line.startsWith("#")) {
                            continue;
                        }
                        result.add(line);
                    }
                }
                return result;
            } catch (Exception e) {
                throw new ApplicationPropertiesException(
                        "Invalid file for '" + key + "', " + e.getMessage(), e);
            }
        }
        else {
            return Splitter.on(",").trimResults().omitEmptyStrings().splitToList(value);
        }
    }

    @Override
    public Iterable<String> split(String key, String delimiter) {
        return Splitter.on(delimiter).trimResults().omitEmptyStrings().split(getString(key));
    }

    @Override
    public Path getPath(String key) {
        String result =  env.getProperty(key);
        if (result == null) {
            throw new ApplicationPropertiesException("Configuration key not found: '" + key + "'");
        }
        return FileUtils.normalize(Paths.get(result.trim()));
    }

    @Override
    public Path getPath(String key, Path path) {
        String result =  env.getProperty(key);
        if (result == null) {
            return path;
        }
        return FileUtils.normalize(Paths.get(result.trim()));
    }

    public int getInt(String key) {
        try {
            return Integer.valueOf(getString(key));
        } catch (Throwable t) {
            throw new ApplicationPropertiesException(t);
        }
    }

    public double getDouble(String key) {
        try {
            return Double.valueOf(getString(key));
        } catch (Throwable t) {
            throw new ApplicationPropertiesException(t);
        }
    }

    public boolean getBoolean(String key) {
        try {
            return Boolean.valueOf(getString(key));
        } catch (Throwable t) {
            throw new ApplicationPropertiesException(t);
        }
    }

    public int getInt(String key, int def) {
        try {
            return Integer.valueOf(getString(key));
        } catch (Throwable t) {
            return def;
        }
    }

    public double getDouble(String key, double def) {
        try {
            return Double.valueOf(getString(key));
        } catch (Throwable t) {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        try {
            return Boolean.valueOf(getString(key));
        } catch (Throwable t) {
            return def;
        }
    }

    public Map<String,Object> getMap(String prefix) {
        Map<String, Object> result = Maps.newHashMap();

        for (PropertySource<?> propertySource: env.getPropertySources()) {
            walkPropertySource(result, prefix, propertySource);
        }
        return result;
    }

    public Properties getProperties(String prefix) {
        return getProperties(prefix, true);
    }

    public Properties getProperties(String prefix, boolean includePrefix) {
        Properties result = new Properties();
        Map<String, Object> map = Maps.newHashMap();

        for (PropertySource<?> propertySource: env.getPropertySources()) {
            walkPropertySource(map, prefix, propertySource);
        }

        if (includePrefix) {
            map.forEach((k, v) -> result.put(k, v.toString()));
        }
        else {
            map.forEach((k, v) -> result.put(k.substring(prefix.length()), v.toString()));
        }
        return result;
    }

    public int max(String key, int value) {
        try {
            return Math.max(Integer.valueOf(getString(key)), value);
        } catch (Throwable t) {
            return value;
        }
    }

    public double max(String key, double value) {
        try {
            return Math.max(Double.valueOf(getString(key)), value);
        } catch (Throwable t) {
            return value;
        }
    }

    public int min(String key, int value) {
        try {
            return Math.min(Integer.valueOf(getString(key)), value);
        } catch (Throwable t) {
            return value;
        }
    }

    public double min(String key, double value) {
        try {
            return Math.min(Double.valueOf(getString(key)), value);
        } catch (Throwable t) {
            return value;
        }
    }

    private void walkPropertySource(Map<String, Object> result, String prefix, PropertySource<?> propSource) {

        if (propSource instanceof CompositePropertySource) {
            CompositePropertySource cps = (CompositePropertySource) propSource;
            cps.getPropertySources().forEach(ps -> walkPropertySource(result, prefix, propSource));
            return;
        }

        if (propSource instanceof EnumerablePropertySource<?>) {
            EnumerablePropertySource<?> ps = (EnumerablePropertySource<?>) propSource;
            Arrays.asList(ps.getPropertyNames() ).stream()
                    .filter(key->key.startsWith(prefix)).forEach(key -> result.put(key, env.getProperty(key)));
        }
    }
}
