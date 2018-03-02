package com.zorroa.archivist.service

import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableMap
import com.google.common.util.concurrent.AbstractScheduledService
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.repository.MaintenanceDao
import com.zorroa.archivist.repository.SharedLinkDao
import com.zorroa.common.config.ApplicationProperties
import org.apache.commons.io.FileUtils
import org.elasticsearch.client.Client
import org.elasticsearch.repositories.RepositoryMissingException
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

/**
 * Created by chambers on 4/21/16.
 */
interface MaintenanceService {

    fun getNextAutomaticBackupFile(): File

    /**
     * Make an online backup of the local H2 SQL DB.  The backup file
     * is named backup_YYYY-MM-dd.zip.
     *
     * @return
     */
    fun automaticBackup(): File?

    /**
     * Make an online backup of the current DB to the given file.
     *
     * @return
     */
    fun backup(file: File)

    /**
     * Remove backups that are older than archivist.maintenance.backups.expireDays.
     * @return
     */
    fun removeExpiredBackups(): Int

    /**
     * Remove backups that are older than the given days argument.
     *
     * @param olderThanDays
     * @return
     */
    fun removeExpiredBackups(olderThanDays: Int): Int

    /**
     * Utilize the archivist.maintenance.jobs.expireDays setting to
     * remove logs and tasks for expired jobs.  The job record is kept
     * around for reference.
     *
     * @return
     */
    fun removeExpiredJobData(): Int

    /**
     * Remove both old tasks and log files that have exceeded the expiration time and return
     * the number of jobs processed.
     */
    fun removeExpiredJobData(olderThan: Long): Int

    fun removeExpiredAnalysts(): Int

    fun removeExpiredSharedLinks(): Int
}

@Service
class MaintenanceServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties,
        private val maintenanceDao: MaintenanceDao,
        private val jobDao: JobDao,
        private val analystDao: AnalystDao,
        private val sharedLinkDao: SharedLinkDao,
        private val client: Client
) : AbstractScheduledService(), MaintenanceService, ApplicationListener<ContextRefreshedEvent> {

    internal var snapshotRepositoryRoot: Path? = null

    internal var repositoryName = "archivist"

    @PostConstruct
    fun init() {
        snapshotRepositoryRoot = properties.getPath("archivist.path.backups")
                .resolve("index")
    }

    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent) {
        /**
         * Not automatically started for unit tests.
         */
        if (properties.getBoolean("archivist.maintenance.backups.enabled")) {
            createElasticSnapshotRepository()
        }

        if (!ArchivistConfiguration.unittest) {
            startAsync()
        }
    }

    fun createElasticSnapshotRepository() {
        var exists = false
        try {
            client.admin().cluster().prepareGetRepositories(repositoryName).get()
            exists = true
        } catch (e: RepositoryMissingException) {
            // ignore
        }

        if (!exists) {
            logger.info("Creating snapshot repository '{}' at '{}'", repositoryName, snapshotRepositoryRoot)
            client.admin().cluster().preparePutRepository(repositoryName)
                    .setType("fs")
                    .setSettings(ImmutableMap.of(
                            "compress", true,
                            "location", snapshotRepositoryRoot.toString())).get()
        } else {
            logger.info("Snapshot repository already exists")
        }
    }

    fun createElasticSnapshot() {

        val time = DateTime()
        val formatter = DateTimeFormat.forPattern("YYYY-MM-dd")
        val snapshotName = formatter.print(time)

        var snapshotExists = false
        try {
            val snapshots = client.admin().cluster().prepareGetSnapshots(repositoryName).get().snapshots
            for (snapshot in snapshots) {
                if (snapshotName == snapshot.name()) {
                    snapshotExists = true
                    break
                }
            }

        } catch (e: RepositoryMissingException) {
            logger.warn("Snapshot repository '{}', does not exist, cannot take snapshot", repositoryName, e)
            return
        }

        if (!snapshotExists) {
            logger.info("creating snapshot: {}", snapshotName)
            try {
                val stopwatch = Stopwatch.createStarted()
                client.admin().cluster()
                        .prepareCreateSnapshot(repositoryName, snapshotName)
                        .setWaitForCompletion(true).get()
                logger.info("created snapshot '{}' in {}", snapshotName, stopwatch)
            } catch (e: Exception) {
                logger.warn("Failed to create snapshot {}", snapshotName, e)
            }

        }
    }

    override fun getNextAutomaticBackupFile(): File {
        val path = properties.getPath("archivist.path.backups").resolve("h2")
        if (!path.toFile().exists()) {
            path.toFile().mkdirs()
        }
        val time = DateTime()
        val formatter = DateTimeFormat.forPattern("YYYY-MM-dd")

        return File(String.format("%s/backup_%s.zip",
                path, formatter.print(time)))
    }

    override fun automaticBackup(): File? {
        /**
         * First we back up everything if we don't have a backup for today
         * already.
         */
        try {
            val fullPath = getNextAutomaticBackupFile()

            /**
             * If our path doesn't exist, do a full backup.
             */
            if (!fullPath.exists()) {
                backup(fullPath)
                return fullPath
            }
        } catch (e: Exception) {
            logger.warn("Failed to to backup data, ", e)
        }

        return null

    }

    override fun backup(file: File) {
        logger.info("Backing up DB to: {}", file)
        maintenanceDao.backup(file)
    }

    fun removeExpiredElasticSnapshots(): Int {
        val now = LocalDate.now()
        try {
            val olderThanDays = properties.getInt("archivist.maintenance.backups.expireDays")
            val snapshots = client.admin().cluster().prepareGetSnapshots(repositoryName).get().snapshots
            for (snapshot in snapshots) {
                val snapshotName = snapshot.name()
                val strDate = snapshotName.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val date = LocalDate.of(
                        Integer.valueOf(strDate[0]),
                        Integer.valueOf(strDate[1]),
                        Integer.valueOf(strDate[2]))

                val daysBetween = DAYS.between(date, now)
                logger.info("Snapshot {} ({}) is {} days old", snapshotName, date, daysBetween)

                if (daysBetween > olderThanDays) {
                    logger.info("Removing snapshot: {}", snapshotName)
                    try {
                        client.admin().cluster().prepareDeleteSnapshot(repositoryName, snapshotName).get()
                    } catch (e: Exception) {
                        logger.warn("Failed to delete snapshot '{}', ", snapshotName, e)
                    }

                }
            }

        } catch (e: Exception) {
            logger.warn("Failed to remove expired job data, ", e)
        }

        return 0
    }

    override fun removeExpiredBackups(): Int {
        try {
            val olderThanDays = properties.getInt("archivist.maintenance.backups.expireDays")
            return removeExpiredBackups(olderThanDays)
        } catch (e: Exception) {
            logger.warn("Failed to remove expired job data, ", e)
        }

        return 0
    }

    override fun removeExpiredBackups(olderThanDays: Int): Int {
        val now = LocalDate.now()

        var result = 0
        val path = properties.getPath("archivist.path.backups").resolve("h2")

        logger.info("Checking backup path: {}", path);
        val file = path.toFile() ?: return result

        for (file in file.listFiles()) {
            try {
                val strDate = file.name.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val date = LocalDate.of(
                        Integer.valueOf(strDate[0]),
                        Integer.valueOf(strDate[1]),
                        Integer.valueOf(strDate[2]))
                val daysBetween = DAYS.between(date, now)
                if (daysBetween >= olderThanDays) {
                    logger.info("Removing file: {}", file)
                    if (file.delete()) {
                        result++
                    } else {
                        logger.warn("Failed to remove file: {}", file)
                    }
                }

            } catch (e: Exception) {
                logger.warn("Unable to determine the date in file: {}", file)
            }

        }
        return result
    }

    override fun removeExpiredJobData(): Int {
        /**
         * Remove expired jobs.
         */
        try {
            val expireDays = properties.getString("archivist.maintenance.jobs.expireDays")
            val expireTime = System.currentTimeMillis() - Integer.valueOf(expireDays) * 86400000
            return removeExpiredJobData(expireTime)
        } catch (e: Exception) {
            logger.warn("Failed to remove expired job data, ", e)
        }

        return 0
    }

    override fun removeExpiredJobData(olderThan: Long): Int {

        val jobs = maintenanceDao.getExpiredJobs(olderThan)
        for (job in jobs) {
            maintenanceDao.removeTaskData(job)
            /**
             * We keep the job around as a record of how date got into
             * the system, so we just set the state to expired.
             */
            jobDao.setState(job, JobState.Expired, null)
        }

        /**
         * Now we do the disk IO.
         */
        for (job in jobs) {
            try {
                FileUtils.deleteDirectory(File(job.rootPath))
            } catch (e: IOException) {
                logger.warn("Failed to delete directory, '" + job.exportedPath + "', ", e)
            }

        }
        return jobs.size
    }

    override fun removeExpiredAnalysts(): Int {
        var result = 0
        val expireTimeMillis = TimeUnit.HOURS.toMillis(
                properties.getInt("archivist.maintenance.analyst.expireHours").toLong())

        for (a in analystDao.getExpired(10, expireTimeMillis)) {
            if (analystDao.delete(a)) {
                result++
            }
        }
        return result
    }

    override fun removeExpiredSharedLinks(): Int {
        val result = sharedLinkDao.deleteExpired(System.currentTimeMillis())
        if (result > 0) {
            logger.info("deleted {} shared link records", result)
        }
        return result
    }

    @Throws(Exception::class)
    override fun runOneIteration() {

        if (properties.getBoolean("archivist.maintenance.backups.enabled")) {

            /**
             * ElasticSearch
             */

            createElasticSnapshot()
            removeExpiredElasticSnapshots()

            /**
             * H2
             */

            /**
             * Do a full backup, then remove old backups.
             */
            automaticBackup()
            removeExpiredBackups()

        } else {
            logger.debug("Backups have been disabled, skipping")
        }

        /**
         * Remove old job data
         */
        removeExpiredJobData()

        /**
         * Remove expired analysts
         */
        removeExpiredAnalysts()

        /**
         * Remove expired shared links.
         */
        removeExpiredSharedLinks()

    }

    override fun scheduler(): AbstractScheduledService.Scheduler {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(1, 60, TimeUnit.MINUTES)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MaintenanceServiceImpl::class.java)
    }
}
