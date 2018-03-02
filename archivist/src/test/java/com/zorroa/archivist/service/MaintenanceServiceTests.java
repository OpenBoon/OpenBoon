package com.zorroa.archivist.service;

import com.google.common.collect.Maps;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.AnalystDao;
import com.zorroa.archivist.repository.MaintenanceDao;
import com.zorroa.common.domain.AnalystSpec;
import com.zorroa.common.domain.AnalystState;
import com.zorroa.sdk.processor.PipelineType;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Created by chambers on 4/21/16.
 */
public class MaintenanceServiceTests extends AbstractTest {

    @Autowired
    MaintenanceService maintenanceService;

    @Autowired
    MaintenanceDao maintenanceDao;

    @Autowired
    JobService jobService;

    @Autowired
    AnalystDao analystDao;

    @Test
    public void testBackup() throws IOException {
        File tmpFile = Files.createTempFile("backup", "zorroa").toFile();
        maintenanceService.backup(tmpFile);
        assertTrue(tmpFile.exists());
        Files.deleteIfExists(tmpFile.toPath());
        assertFalse(tmpFile.exists());
    }

    @Test
    public void testAutomaticBackup() throws IOException {
        String vendor = properties.getString("archivist.datasource.primary.vendor");
        if (!vendor.equals("h2")) { return; }

        File file = maintenanceService.getNextAutomaticBackupFile();
        if (file.exists()) {
            Files.delete(file.toPath());
        }
        File backup = maintenanceService.automaticBackup();
        assertTrue(backup.exists());
        Files.deleteIfExists(backup.toPath());
    }

    @Test
    public void testRemoveExpiredBackups() throws IOException {
        String vendor = properties.getString("archivist.datasource.primary.vendor");
        if (!vendor.equals("h2")) { return; }

        try {
            maintenanceService.removeExpiredBackups(0);
            File file = maintenanceService.getNextAutomaticBackupFile();
            if (file != null) {
                if (file.exists()) {
                    Files.delete(file.toPath());
                }
            }

            File backup = maintenanceService.automaticBackup();
            assertTrue(backup.exists());
            assertEquals(1, maintenanceService.removeExpiredBackups(0));
            assertFalse(backup.exists());
        } catch (Exception e) {
            logger.warn("Failed to remove expired backups", e);
            assertTrue(false);
        }
    }

    @Test
    public void testRemoveExpiredJobData() {
        JobSpec jspec = new JobSpec();
        jspec.setType(PipelineType.Import);
        jspec.setName("test");
        Job job = jobService.launch(jspec);

        jobService.createTask(new TaskSpec()
                .setJobId(jspec.getJobId())
                .setScript(new ZpsScript())
                .setName("test"));

        jobService.setJobState(job, JobState.Finished, JobState.Active);
        long time = System.currentTimeMillis()+1000;
        ExpiredJob ejob = maintenanceDao.getExpiredJobs(time).get(0);

        assertEquals("Wrong job id", job.getJobId(), ejob.getJobId());
        assertTrue("Log directory does not exist", new File(ejob.getLogPath()).exists());
        assertEquals(1, (int) jdbc.queryForObject("SELECT COUNT(1) FROM task WHERE pk_job=?",
                Integer.class, job.getId()));
        assertEquals(1, (int) jdbc.queryForObject("SELECT COUNT(1) FROM task_stat WHERE pk_job=?",
                Integer.class, job.getId()));

        assertEquals(1, maintenanceService.removeExpiredJobData(time));
        assertFalse("Root directory should not exist", new File(ejob.getRootPath()).exists());
        assertFalse("Log directory should not exist", new File(ejob.getLogPath()).exists());
        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(1) FROM task WHERE pk_job=?",
                Integer.class, job.getId()));
        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(1) FROM task_stat WHERE pk_job=?",
                Integer.class, job.getId()));

        Job updateJob = jobService.get(job.getId());
        assertEquals(JobState.Expired, updateJob.getState());

    }

    @Test
    public void testRemoveExpiredAnalysts() {
        assertEquals(0, maintenanceService.removeExpiredAnalysts());

        AnalystSpec spec = new AnalystSpec();
        spec.setId("bilbo");
        spec.setState(AnalystState.DOWN);
        spec.setUrl("http://127.0.0.2:8099");
        spec.setQueueSize(1);
        spec.setMetrics(Maps.newHashMap());
        spec.setArch("osx");
        spec.setUpdatedTime(1);
        String id = analystDao.register(spec);
        refreshIndex();

        assertEquals(1, maintenanceService.removeExpiredAnalysts());
    }

}
