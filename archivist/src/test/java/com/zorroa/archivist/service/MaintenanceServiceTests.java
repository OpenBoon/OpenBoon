package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 4/21/16.
 */
public class MaintenanceServiceTests extends AbstractTest {

    @Autowired
    MaintenanceService maintenanceService;

    @Test
    public void testBackup() throws IOException {
        File file = null;
        try {
            file = maintenanceService.backup();
            assertTrue(file.exists());
        } finally {
            if (file != null) {
                Files.deleteIfExists(file.toPath());
            }
        }
    }

    @Test
    public void testRemoveExpiredJobData() {
        maintenanceService.removeExpiredJobData();
    }
}
