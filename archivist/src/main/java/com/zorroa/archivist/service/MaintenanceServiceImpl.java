package com.zorroa.archivist.service;

/**
 * Created by chambers on 4/21/16.
 */

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.ExpiredJob;
import com.zorroa.archivist.domain.JobState;
import com.zorroa.archivist.repository.AnalystDao;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.repository.MaintenanceDao;
import com.zorroa.archivist.repository.SharedLinkDao;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.Analyst;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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

    @Autowired
    AnalystDao analystDao;

    @Autowired
    SharedLinkDao sharedLinkDao;

    @Autowired
    Client client;

    Path snapshotRepositoryRoot;

    String repositoryName = "archivist";

    @PostConstruct
    public void init() {
        snapshotRepositoryRoot =
                properties.getPath("archivist.path.backups")
                        .resolve("index");

        if (properties.getBoolean("archivist.maintenance.backups.enabled")) {
            createElasticSnapshotRepository();
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        /**
         * Not automatically started for unit tests.
         */
        if (!ArchivistConfiguration.unittest) {
            startAsync();
        }
    }

    public void createElasticSnapshotRepository() {
        boolean exists = false;
        try {
            client.admin().cluster().prepareGetRepositories(repositoryName).get();
            exists = true;
        } catch (RepositoryMissingException e) {
            // ignore
        }

        if (!exists) {
            logger.info("Creating snapshot repository '{}' at '{}'", repositoryName, snapshotRepositoryRoot);
            client.admin().cluster().preparePutRepository(repositoryName)
                    .setType("fs")
                    .setSettings(ImmutableMap.of(
                            "compress", true,
                            "location", snapshotRepositoryRoot.toString())).get();
        }
        else {
            logger.info("Snapshot repository already exists");
        }
    }

    public void createElasticSnapshot() {

        DateTime time = new DateTime();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY-MM-dd");
        String snapshotName = formatter.print(time);

        boolean snapshotExists = false;
        try {
            List<SnapshotInfo> snapshots =
                    client.admin().cluster().prepareGetSnapshots(repositoryName).get().getSnapshots();
            for(SnapshotInfo snapshot :snapshots) {
                if (snapshotName.equals(snapshot.name())) {
                    snapshotExists = true;
                    break;
                }
            }

        } catch (RepositoryMissingException e) {
            logger.warn("Snapshot repository '{}', does not exist, cannot take snapshot", repositoryName, e);
            return;
        }

        if (!snapshotExists) {
            logger.info("creating snapshot: {}",snapshotName);
            try {
                Stopwatch stopwatch = Stopwatch.createStarted();
                client.admin().cluster()
                        .prepareCreateSnapshot(repositoryName, snapshotName)
                        .setWaitForCompletion(true).get();
                logger.info("created snapshot '{}' in {}", snapshotName, stopwatch);
            } catch (Exception e) {
                logger.warn("Failed to create snapshot {}", snapshotName, e);
            }
        }
    }

    @Override
    public File getNextAutomaticBackupFile() {
        Path path = properties.getPath("archivist.path.backups").resolve("h2");
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }
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

    public int removeExpiredElasticSnapshots() {
        LocalDate now = LocalDate.now();
        try {
            int olderThanDays = properties.getInt("archivist.maintenance.backups.expireDays");
            List<SnapshotInfo> snapshots =
                    client.admin().cluster().prepareGetSnapshots(repositoryName).get().getSnapshots();
            for(SnapshotInfo snapshot :snapshots) {
                String snapshotName = snapshot.name();
                String strDate[] = snapshotName.split("-");
                LocalDate date = LocalDate.of(
                        Integer.valueOf(strDate[0]),
                        Integer.valueOf(strDate[1]),
                        Integer.valueOf(strDate[2]));

                long daysBetween = DAYS.between(date, now);
                logger.info("Snapshot {} ({}) is {} days old", snapshotName, date, daysBetween);

                if (daysBetween > olderThanDays) {
                    logger.info("Removing snapshot: {}", snapshotName);
                    try {
                        client.admin().cluster().prepareDeleteSnapshot(repositoryName, snapshotName).get();
                    } catch (Exception e) {
                        logger.warn("Failed to delete snapshot '{}', ", snapshotName, e);
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("Failed to remove expired job data, ", e);
        }
        return 0;
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
        Path path = properties.getPath("archivist.path.backups").resolve("h2");
        for (File file: path.toFile().listFiles()) {
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
    public int removeExpiredAnalysts() {
        int result = 0;
        long expireTimeMillis = TimeUnit.HOURS.toMillis(
                properties.getInt("archivist.maintenance.analyst.expireHours"));

        for (Analyst a: analystDao.getExpired(10, expireTimeMillis)) {
            if (analystDao.delete(a)) {
                result++;
            }
        }
        return result;
    }

    @Override
    public int removeExpiredSharedLinks() {
        int result = sharedLinkDao.deleteExpired(System.currentTimeMillis());
        if (result > 0) {
            logger.info("deleted {} shared link records", result);
        }
        return result;
    }

    @Override
    protected void runOneIteration() throws Exception {

        if (properties.getBoolean("archivist.maintenance.backups.enabled")) {

            /**
             * ElasticSearch
             */

            createElasticSnapshot();
            removeExpiredElasticSnapshots();

            /**
             * H2
             */

            /**
             * Do a full backup, then remove old backups.
             */
            automaticBackup();
            removeExpiredBackups();

        }
        else {
            logger.debug("Backups have been disabled, skipping");
        }

        /**
         * Remove old job data
         */
        removeExpiredJobData();

        /**
         * Remove expired analysts
         */
        removeExpiredAnalysts();

        /**
         * Remove expired shared links.
         */
        removeExpiredSharedLinks();

    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule(1, 60, TimeUnit.MINUTES);
    }
}
