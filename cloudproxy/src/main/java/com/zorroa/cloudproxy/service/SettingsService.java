package com.zorroa.cloudproxy.service;

import com.zorroa.cloudproxy.domain.Settings;
import com.zorroa.cloudproxy.domain.ImportStatus;

import java.io.IOException;

/**
 * Created by chambers on 3/27/17.
 */

public interface SettingsService {

    Settings saveSettings(Settings props) throws IOException;

    Settings getSettings();

    boolean saveImportStats(ImportStatus last);

    ImportStatus getImportStats();
}
