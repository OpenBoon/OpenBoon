package com.zorroa.archivist.service;

import com.google.common.collect.Maps;
import com.zorroa.archivist.repository.SettingsDao;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by chambers on 5/30/17.
 */
@Service
public class SettingsServiceImpl implements SettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SettingsServiceImpl.class);

    @Autowired
    SettingsDao settingsDao;

    /**
     * A whitelist of property names that can be set via the API.
     */
    private static final Map<String, Class<?>> WHITELIST = Maps.newHashMap();
    static {
        WHITELIST.put("archivist\\.search\\.keywords\\.field\\..+", Double.class);
        WHITELIST.put("archivist\\.search\\.keywords\\.auto\\.enabled", Boolean.class);
        WHITELIST.put("archivist\\.search\\.keywords\\.auto\\.fieldNames", String.class);
        WHITELIST.put("archivist\\.search\\.queryAnalyzer", String.class);
    }

    @PostConstruct
    public void init() {
        /**
         * Restore the runtime settings.
         */
        Map<String,String> settings = settingsDao.getAll();
        for (Map.Entry<String,String> e: settings.entrySet()) {
            System.setProperty(e.getKey(), e.getValue());
        }
    }

    @Override
    public void setAll(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry: values.entrySet()) {
            if (!isValid(entry.getKey(), entry.getValue())) {
                throw new ArchivistWriteException("Invalid setting: '" +
                        entry.getKey() + ", either type or setting is not allowed.");
            }

            if (entry.getValue() == null) {
                settingsDao.unset(entry.getKey());
                System.clearProperty(entry.getKey());
            }
            else {
                String value = String.valueOf(entry.getValue());
                settingsDao.set(entry.getKey(), value);
                System.setProperty(entry.getKey(), value);
            }
        }
    }

    @Override
    public Map<String, String> getAll() {
        return settingsDao.getAll();
    }

    @Override
    public boolean isValid(String key, Object value) {
        for (Map.Entry<String, Class<?>> pattern: WHITELIST.entrySet()) {
            if (Pattern.matches(pattern.getKey(), key) && pattern.getValue().isInstance(value)) {
                return true;
            }
        }
        return false;
    }
}
