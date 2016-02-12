package com.zorroa.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * Created by chambers on 2/12/16.
 */
public class ApplicationProperties {

    @Autowired
    Environment env;

    public ApplicationProperties() { }

    public String getString(String key) {
        String result =  env.getProperty(key);
        if (result == null) {
            throw new ApplicationPropertiesException("Configuration key not found: '" + key + "'");
        }
        return result;
    }

    public <T> T get(String key, T def) {
       return (T) env.getProperty(key, def.getClass());
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
}
