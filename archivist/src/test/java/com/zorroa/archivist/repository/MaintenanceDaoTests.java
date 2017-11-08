package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobSpec;
import com.zorroa.archivist.domain.JobState;
import com.zorroa.archivist.service.JobService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.zorroa.archivist.domain.PipelineType.Import;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 4/21/16.
 */
public class MaintenanceDaoTests extends AbstractTest {

    @Autowired
    MaintenanceDao maintenanceDao;

    @Autowired
    JobService jobService;

    File file = new File("backups/test-backup.zip");

    @After
    public void after() throws IOException {
        logger.info("removing test file");
        Files.deleteIfExists(file.toPath());
    }

    @Test
    public void testBackup() {
        if (!((AbstractDao) maintenanceDao).isDbVendor("h2")) {
            return;
        }
        maintenanceDao.backup(file);
        assertTrue(file.exists());
    }

    @Test
    public void getExpiredJobs() {
        JobSpec jspec = new JobSpec();
        jspec.setType(Import);
        jspec.setName("test");
        Job job = jobService.launch(jspec);
        jobService.setJobState(job, JobState.Finished, JobState.Active);

        assertEquals(0, maintenanceDao.getExpiredJobs(System.currentTimeMillis()-1000).size());
        assertEquals(1, maintenanceDao.getExpiredJobs(System.currentTimeMillis()+1000).size());
    }

}
