package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 4/21/16.
 */
public class MaintenanceDaoTests extends AbstractTest {

    @Autowired
    MaintenanceDao maintenanceDao;

    File file = new File("backups/test-backup.zip");

    @After
    public void after() throws IOException {
        logger.info("removing test file");
        Files.deleteIfExists(file.toPath());
    }

    @Test
    public void testBackup() {
        maintenanceDao.backup(file);
        assertTrue(file.exists());
    }
}
