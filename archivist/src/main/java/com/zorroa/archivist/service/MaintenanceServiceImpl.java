package com.zorroa.archivist.service;

/**
 * Created by chambers on 4/21/16.
 */

import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.domain.ExpiredJob;
import com.zorroa.archivist.domain.JobState;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.repository.MaintenanceDao;
import com.zorroa.common.config.ApplicationProperties;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class MaintenanceServiceImpl extends AbstractScheduledService
        implements MaintenanceService, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    MaintenanceDao maintenanceDao;

    @Autowired
    JobDao jobDao;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        /**
         * Not automatically started for unit tests.
         */
        if (!ArchivistConfiguration.unittest) {
            startAsync();
        }
    }

    @Override
    public File getNextAutomaticBackupFile() {
        String path = properties.getString("archivist.path.backups");
        DateTime time = new DateTime();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY-MM-dd");

        File fullPath = new File(String.format("%s/backup_%s.zip",
                path, formatter.print(time)));
        return fullPath;
    }

    @Override
    public File automaticBackup() {
        /**
         * First we back up everything if we don't have a backup for today
         * already.
         */
        try {
            File fullPath = getNextAutomaticBackupFile();

            /**
             * If our path doesn't exist, do a full backup.
             */
            if (!fullPath.exists()) {
                backup(fullPath);
                return fullPath;
            }
        } catch (Exception e) {
            logger.warn("Failed to to backup data, ", e);
        }

        return null;

    }
    @Override
    public void backup(File file) {
        logger.info("Backing up DB to: {}", file);
        maintenanceDao.backup(file);
    }

    @Override
    public int removeExpiredBackups() {
        try {
            int olderThanDays = properties.getInt("archivist.maintenance.backups.expireDays");
            return removeExpiredBackups(olderThanDays);
        } catch (Exception e) {
            logger.warn("Failed to remove expired job data, ", e);
        }
        return 0;
    }

    @Override
    public int removeExpiredBackups(int olderThanDays) {
        LocalDate now = LocalDate.now();

        int result = 0;
        String path = properties.getString("archivist.path.backups");
        for (File file: new File(path).listFiles()) {
            try {
                String strDate[] = file.getName().split("_")[1].split("\\.")[0].split("-");
                LocalDate date = LocalDate.of(
                        Integer.valueOf(strDate[0]),
                        Integer.valueOf(strDate[1]),
                        Integer.valueOf(strDate[2]));
                long daysBetween = DAYS.between(date, now);
                if (daysBetween >= olderThanDays) {
                    logger.info("Removing file: {}", file);
                    if (file.delete()) {
                        result++;
                    }
                    else {
                        logger.warn("Failed to remove file: {}", file);
                    }
                }

            } catch (Exception e) {
                logger.warn("Unable to determine the date in file: {}", file);
            }
        }
        return result;
         }

    @Override
    public int removeExpiredJobData() {
        /**
         * Remove expired jobs.
         */
        try {
            String expireDays = properties.getString("archivist.maintenance.jobs.expireDays");
            long expireTime = System.currentTimeMillis() - (Integer.valueOf(expireDays) * 86400000);
            return removeExpiredJobData(expireTime);
        } catch (Exception e) {
            logger.warn("Failed to remove expired job data, ", e);
        }

        return 0;
    }

    @Override
    public int removeExpiredJobData(long olderThan) {

        List<ExpiredJob> jobs = maintenanceDao.getExpiredJobs(olderThan);
        for (ExpiredJob job: jobs) {
            maintenanceDao.removeTaskData(job);
            /**
             * We keep the job around as a record of how date got into
             * the system, so we just set the state to expired.
             */
            jobDao.setState(job, JobState.Expired, null);
        }

        /**
         * Now we do the disk IO.
         */
        for (ExpiredJob job: jobs) {
            try {
                FileUtils.deleteDirectory(new File(job.getLogPath()));
            } catch (IOException e) {
                logger.warn("Failed to delete directory, '" + job.getLogPath() + "', ", e);
            }

            try {
                FileUtils.deleteDirectory(new File(job.getExportedPath()));
            } catch (IOException e) {
                logger.warn("Failed to delete directory, '" + job.getExportedPath() + "', ", e);
            }
        }

        return jobs.size();
    }

    @Override
    protected void runOneIteration() throws Exception {

        /**
         * Do a full backup, then remove old backups.
         */
        automaticBackup();
        removeExpiredBackups();

        /**
         * Remove old job data
         */
        removeExpiredJobData();
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(10, 60, TimeUnit.MINUTES);
    }
}
