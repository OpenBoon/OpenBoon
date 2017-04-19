package com.zorroa.cloudproxy.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.cloudproxy.domain.ImportStatus;
import com.zorroa.cloudproxy.domain.ImportTask;
import com.zorroa.cloudproxy.domain.Settings;
import com.zorroa.sdk.client.ArchivistClient;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by chambers on 3/24/17.
 */
@Component
public class SchedulerServiceImpl implements SchedulerService, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    @Value("${cloudproxy.paths.config}")
    private String configPath;

    @Value("${cloudproxy.paths.shared}")
    private String sharedPath;

    @Value("${cloudproxy.run-at-startup}")
    private boolean runAtStartup;

    @Autowired
    SettingsService configService;

    ThreadPoolTaskScheduler cronScheduler;

    CronTrigger trigger;

    List<Future<ImportTask>> pendingTasks = Collections.synchronizedList(Lists.newArrayList());

    public SchedulerServiceImpl() {
        cronScheduler = makeScheduler();
    }

    @Override
    public Date getNextRunTime() {
        if (trigger == null) {
            return null;
        }
        ImportStatus stats = configService.getImportStats();
        SimpleTriggerContext ctx = new SimpleTriggerContext();
        ctx.update(new Date(stats.getStartTime() > 0 ? stats.getStartTime(): System.currentTimeMillis()),
                new Date(stats.getStartTime()),
                new Date(stats.getFinishTime()));
        return trigger.nextExecutionTime(ctx);
    }

    @Override
    public void reloadAndRestart(boolean allowStartNow) {

        Settings configProps = configService.getSettings();
        if (configProps == null) {
            return;
        }

        /**
         * Cancel all pending tasks.
         */
        for (Future<ImportTask> future: pendingTasks) {
            future.cancel(true);
        }
        pendingTasks.clear();

        /**
         * Apply the schedule.
         */
        if (configProps.getSchedule() != null) {
            logger.info("Creating schedule using: {}", configProps.getSchedule());
            trigger = new CronTrigger(configProps.getSchedule());
            Future task = cronScheduler.schedule(()-> executeImportTask(true), trigger);
            pendingTasks.add(task);
        }

        if (allowStartNow && configProps.isStartNow()) {
            Future task = cronScheduler.submit(()-> executeImportTask(true));
            pendingTasks.add(task);
        }
    }

    @Override
    public ImportTask executeImportTask(boolean cleanup) {

        Settings configProps = configService.getSettings();
        if (configProps == null) {
            return null;
        }

        ImportStatus nextRun = configService.getImportStats();
        logger.info("Last run: {}", nextRun.getStartTime());
        logger.info("Next run: {}", nextRun.getNextTime());

        String scriptFile = configPath + "/script.zps";
        try {

            /**
             * Set some system props that the archivist client can grab.
             */
            System.setProperty("zorroa.hmac.key", configProps.getHmacKey());
            System.setProperty("zorroa.user", configProps.getAuthUser());

            ArchivistClient client = new ArchivistClient();
            Map<String, Object> job = client.getConnection().post("/api/v1/jobs",
                    ImmutableMap.of(
                            "name", "CloudProxyImport",
                            "type", "Import",
                            "script", new ZpsScript()), Map.class);

            nextRun.setStartTime(System.currentTimeMillis());
            nextRun.setLastJobId(nextRun.getCurrentJobId());
            nextRun.setCurrentJobId((Integer) job.get("jobId"));
            nextRun.setActive(true);
            configService.saveImportStats(nextRun);

            ImportTask task = new ImportTask(scriptFile, sharedPath, configProps, nextRun);
            task.start();

            return task;
        } catch (Exception e) {
            throw new RuntimeException("Unable to start cloud proxy process, ", e);
        }
        finally {
            logger.info("Task is complete");
            try {
                ImportStatus lastRun = configService.getImportStats();
                lastRun.setFinishTime(System.currentTimeMillis());
                lastRun.setActive(false);
                configService.saveImportStats(lastRun);
            } catch (Exception e) {
                logger.warn("Failed to save last run data, ", e);
            }
        }
    }

    @Override
    public void cleanupImportTaskWorkDir(ImportTask task) throws FileNotFoundException {
        logger.info("Deleting work dir: {}", task.getWorkDir());
        FileUtils.deleteRecursive(task.getWorkDir().toFile());
    }

    private static final ThreadPoolTaskScheduler makeScheduler() {
        ThreadPoolTaskScheduler exec = new ThreadPoolTaskScheduler();
        exec.setPoolSize(1);
        exec.setRemoveOnCancelPolicy(true);
        exec.setWaitForTasksToCompleteOnShutdown(false);
        exec.setAwaitTerminationSeconds(60);
        exec.initialize();
        return exec;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        reloadAndRestart(runAtStartup);
    }
}
