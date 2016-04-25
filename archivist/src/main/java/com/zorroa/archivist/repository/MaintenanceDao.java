package com.zorroa.archivist.repository;

import java.io.File;

/**
 * Created by chambers on 4/21/16.
 */
public interface MaintenanceDao {
    void backup(File file);
}
