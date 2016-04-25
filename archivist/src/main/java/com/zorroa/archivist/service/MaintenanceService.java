package com.zorroa.archivist.service;

import java.io.File;

/**
 * Created by chambers on 4/21/16.
 */
public interface MaintenanceService {

    /**
     * Make an online backup of the current DB and return.
     *
     * @return
     */
     File backup();
}
