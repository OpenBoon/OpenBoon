package com.zorroa.cloudproxy.service;

import com.zorroa.cloudproxy.domain.ImportTask;
import com.zorroa.cloudproxy.domain.ImportStats;
import com.zorroa.cloudproxy.domain.Settings;
import com.zorroa.sdk.util.FileUtils;
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
import java.util.Date;

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

    @Override
    public Date getNextRunTime() {
        if (trigger == null) {
            return null;
        }
        ImportStats stats = configService.getImportStats();
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

        shutdownScheduler();
        cronScheduler = makeScheduler();

        if (configProps.getSchedule() != null) {
            logger.info("Creating schedule using: {}", configProps.getSchedule());
            trigger = new CronTrigger(configProps.getSchedule());
            cronScheduler.schedule(()-> startImportTask(true), trigger);
        }
        if (allowStartNow && configProps.isStartNow()) {
            cronScheduler.execute(()-> startImportTask(true));
        }

    }

    @Override
    public ImportTask startImportTask(boolean cleanup) {

        Settings configProps = configService.getSettings();
        if (configProps == null) {
            return null;
        }

        ImportStats lastRun = configService.getImportStats();
        String scriptFile = configPath + "/script.zps";
        ImportTask task = new ImportTask(scriptFile, sharedPath, configProps, lastRun);
        try {
            lastRun.setStartTime(System.currentTimeMillis());
            configService.saveImportStats(lastRun);
            task.start();
        } finally {
            try {
                lastRun = configService.getImportStats();
                lastRun.setFinishTime(System.currentTimeMillis());
                configService.saveImportStats(lastRun);

                logger.info("Saving last run...");
                configService.saveImportStats(lastRun);
            } catch (Exception e) {
                logger.warn("Failed to save last run data, ", e);
            }
            /*
            if (cleanup) {
                try {
                    cleanupImportTaskWorkDir(task);
                } catch (Exception e) {
                    logger.warn("Failed to cleanup import data, ", e);
                }
            }*/
        }

        return task;
    }

    @Override
    public void cleanupImportTaskWorkDir(ImportTask task) throws FileNotFoundException {
        logger.info("Deleting work dir: {}", task.getWorkDir());
        FileUtils.deleteRecursive(task.getWorkDir().toFile());
    }

    private void shutdownScheduler() {
        if (cronScheduler != null) {
            logger.info("Shutting down existing scheduler with {} active, {} waiting tasks",
                    cronScheduler.getActiveCount(),
                    cronScheduler.getScheduledThreadPoolExecutor().getQueue().size());
            cronScheduler.shutdown();
            cronScheduler = null;
        }
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
