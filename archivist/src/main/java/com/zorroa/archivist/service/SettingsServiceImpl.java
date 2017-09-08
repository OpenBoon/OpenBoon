package com.zorroa.archivist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.Setting;
import com.zorroa.archivist.domain.SettingsFilter;
import com.zorroa.archivist.repository.SettingsDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.client.exception.ArchivistReadException;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.client.exception.MissingElementException;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Provides a clear picture of all settings on the server.
 */
@Service
public class SettingsServiceImpl implements SettingsService, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SettingsServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    SettingsDao settingsDao;

    // a memoizer would be nicer but no good ones that allow manual invalidation
    private final LoadingCache<Integer, List<Setting>> settingsCache = CacheBuilder.newBuilder()
            .maximumSize(2)
            .initialCapacity(2)
            .concurrencyLevel(1)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<Integer, List<Setting>>() {
                public List<Setting> load(Integer key) throws Exception {
                    return settingsProvider();
                }
            });

    /**
     * A whitelist of property names that can be set via the API.
     */
    private static final Map<String, TypeReference> WHITELIST =
            ImmutableMap.<String, TypeReference>builder()
        .put("archivist.search.keywords.static.fields", new TypeReference<Map<String,Float>>() {})
        .put("archivist.search.keywords.auto.fields", new TypeReference<String>() {})
        .put("archivist.search.keywords.auto.enabled", new TypeReference<Boolean>() {})
        .put("archivist.export.dragTemplate", new TypeReference<String>(){})
        .build();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        /**
         * Restore the runtime settings.
         */
        Map<String,String> settings = settingsDao.getAll();
        for (Map.Entry<String,String> e: settings.entrySet()) {
            System.setProperty(e.getKey(), e.getValue());
        }
    }

    @Override
    public int setAll(Map<String, Object> values) {
        int result = 0;
        for (Map.Entry<String, Object> entry: values.entrySet()) {
            String val;
            if (entry.getValue() instanceof Map) {
                val = Json.serializeToString(entry.getValue());
            }
            else {
                val = entry.getValue().toString();
            }
            if (set(entry.getKey(), val, false)) {
                result++;
            }
        }
        settingsCache.invalidateAll();
        return result;
    }

    @Override
    public List<Setting> getAll(SettingsFilter filter) {
        if (!SecurityUtils.hasPermission("group::administrator",
                "group::developer")) {
            filter.setLiveOnly(true);
        }
        try {
            return settingsCache.get(0).stream()
                    .filter(s->filter.matches(s))
                    .limit(filter.getCount())
                    .collect(Collectors.toList());
        } catch (ExecutionException e) {
            throw new ArchivistReadException(e);
        }
    }

    @Override
    public List<Setting> getAll() {
        return getAll(new SettingsFilter());
    }

    @Override
    public Setting get(String key) {
        SettingsFilter filter = new SettingsFilter();
        filter.setCount(1);
        filter.setNames(ImmutableSet.of(key));
        if (!SecurityUtils.hasPermission("group::administrator", "group::developer")) {
            filter.setLiveOnly(true);
        }
        try {
            return getAll(filter).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new MissingElementException("Setting not found: " + key, e);
        }
    }

    @Override
    public void checkValid(String key, String value) {
        for (Map.Entry<String, TypeReference> pattern: WHITELIST.entrySet()) {
            if (pattern.getKey().equals(key)) {
                try {
                    Json.deserialize(value, pattern.getValue());
                    return;
                } catch (Exception e ){
                    throw new ArchivistWriteException(
                            "Invalid value for " + key + ", invalid type.");
                }
            }
        }
        throw new ArchivistWriteException(
                "Failed to set value for key " + key + ", invalid key.");
    }

    @Override
    public boolean set(String key, String value) {
        return set(key, value, true);
    }

    public boolean set(String key, String value, boolean invalidate) {
        checkValid(key, value);

        logger.info("{} changed to {} by {}", key, value, SecurityUtils.getUsername());

        boolean result;
        if (value == null) {
            result = settingsDao.unset(key);
            System.clearProperty(key);
        }
        else {
            result = settingsDao.set(key, value);
            System.setProperty(key, value);
        }
        if (invalidate) {
            settingsCache.invalidateAll();
        }
        return result;
    }

    private boolean isLive(String key) {
        return WHITELIST.keySet().contains(key);
    }

    public List<Setting> settingsProvider() {

        Resource resource = new ClassPathResource("/application.properties");
        List<Setting> result = Lists.newArrayListWithExpectedSize(64);

        String description = null;
        String category = null;
        String property;
        String value;
        String line;

        try (
                InputStreamReader isr = new InputStreamReader(resource.getInputStream(), Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr)
        ) {
            while ((line = br.readLine()) != null) {

                if (line.isEmpty()) {
                    continue;
                } else if (line.startsWith("###")) {
                    if (description != null) {
                        description = description.concat(line.substring(3));
                    } else {
                        description = line.substring(3);
                    }
                    description = description.trim();
                } else if (line.startsWith("##")) {
                    category = line.substring(2).trim();

                } else if (!line.startsWith("#") && line.contains("=")) {
                    List<String> e = Splitter.on('=').trimResults().omitEmptyStrings().splitToList(line);
                    property = e.get(0);
                    if (property.contains("password") || property.contains("secret")) {
                        value = "<HIDDEN>";
                    } else {
                        try {
                            value = e.get(1);
                        } catch (IndexOutOfBoundsException ex) {
                            value = null;
                        }
                    }

                    String currentValue;
                    if ("<HIDDEN>".equals(value)) {
                        currentValue = "<HIDDEN>";
                    }
                    else {
                        currentValue = properties.getString(property);
                    }

                    Setting s = new Setting();
                    s.setName(property);
                    s.setDefaultValue(value);
                    s.setCurrentValue(currentValue);
                    s.setLive(isLive(property));
                    s.setCategory(category);
                    s.setDescription(description);
                    result.add(s);

                    description = null;
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to read default properties file,", e);
        }
        return result;
    }


}
