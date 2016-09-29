package com.zorroa.archivist.service;

import java.io.File;

/**
 * Created by chambers on 4/21/16.
 */
public interface MaintenanceService {

    File getNextAutomaticBackupFile();

    /**
     * Make an online backup of the local H2 SQL DB.  The backup file
     * is named backup_YYYY-MM-dd.zip.
     *
     * @return
     */
    File automaticBackup();

    /**
     * Make an online backup of the current DB to the given file.
     *
     * @return
     */
     void backup(File file);

    /**
     * Remove backups that are older than archivist.maintenance.backups.expireDays.
     * @return
     */
    int removeExpiredBackups();

    /**
     * Remove backups that are older than the given days argument.
     *
     * @param olderThanDays
     * @return
     */
    int removeExpiredBackups(int olderThanDays);

    /**
     * Utilize the archivist.maintenance.jobs.expireDays setting to
     * remove logs and tasks for expired jobs.  The job record is kept
     * around for reference.
     *
     * @return
     */
    int removeExpiredJobData();

    /**
     * Remove both old tasks and log files that have exceeded the expiration time and return
     * the number of jobs processed.
     */
    int removeExpiredJobData(long olderThan);
}
