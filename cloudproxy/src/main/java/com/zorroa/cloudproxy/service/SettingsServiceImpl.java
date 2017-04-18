package com.zorroa.cloudproxy.service;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.zorroa.cloudproxy.domain.Settings;
import com.zorroa.cloudproxy.domain.ImportStatus;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * Created by chambers on 3/27/17.
 */
@Component
public class SettingsServiceImpl implements SettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SettingsServiceImpl.class);

    @Value("${cloudproxy.paths.config}")
    private String configPath;

    private String settingsFile;
    private String statsFile;

    private Settings configSettings;

    /**
     * Load config at startup.
     */
    @PostConstruct
    public void init() {
        Json.Mapper.enable(SerializationFeature.INDENT_OUTPUT);
        settingsFile = configPath + "/config.json";
        statsFile = configPath + "/stats.json";
        loadConfig();
    }

    @Override
    public Settings saveSettings(Settings props) throws IOException {
        Json.Mapper.writeValue(new File(settingsFile), props);
        configSettings = props;
        logger.info("New configuration saved to: {}", settingsFile);
        return props;
    }

    @Override
    public Settings getSettings() {
        return configSettings;
    }

    /**
     * Looks for configuration files are being setup via web interface.
     */
    public Settings loadConfig() {
        logger.info("loading configuration: {}", settingsFile);
        File file = new File(settingsFile);
        if (!file.exists()) {
            return null;
        }

        try {
            configSettings = Json.Mapper.readValue(file, Settings.class);
            return configSettings;
        } catch (IOException e) {
            logger.warn("Unable to load configuration, unexpected error: ", e);
        }
        return null;
    }

    @Override
    public boolean saveImportStats(ImportStatus last) {
        try {
            Json.Mapper.writeValue(new File(statsFile), last);
            return true;
        } catch (IOException e) {
            logger.warn("Unable to save configuration, unexpected error: ", e);
        }
        return false;
    }

    @Override
    public ImportStatus getImportStats() {
        File file = new File(statsFile);
        if (!file.exists()) {
            logger.warn("Import stats file does not exist!");
            return null;
        }

        try {
            return Json.Mapper.readValue(file, ImportStatus.class);
        } catch (IOException e) {
            logger.warn("Unable to load last run data, unexpected error: ", e);
        }
        return null;
    }
}
