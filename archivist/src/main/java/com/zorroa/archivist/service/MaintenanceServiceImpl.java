package com.zorroa.archivist.service;

/**
 * Created by chambers on 4/21/16.
 */

import com.zorroa.archivist.repository.MaintenanceDao;
import com.zorroa.sdk.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class MaintenanceServiceImpl implements MaintenanceService {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    MaintenanceDao maintenanceDao;

    @Override
    public File backup() {
        String path = properties.getString("archivist.path.backups");
        for(;;) {
            File fullPath =new File(String.format("%s/backup-%d.zip",
                    path, System.currentTimeMillis() / 1000));
            if (!fullPath.exists()) {
                maintenanceDao.backup(fullPath);
                return fullPath;
            }

            logger.warn("Backup '{}' already exists, waiting 1 second", fullPath);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("Failed to determine filename for DB backup,", e);
                return null;
            }
        }
    }
}
