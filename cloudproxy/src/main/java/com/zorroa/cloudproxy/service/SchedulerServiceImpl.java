package com.zorroa.cloudproxy.service;

import com.zorroa.cloudproxy.domain.ImportStatus;
import com.zorroa.cloudproxy.domain.Settings;
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

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

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

    @Autowired
    ImportTaskService importTaskService;

    ThreadPoolTaskScheduler cronScheduler;

    CronTrigger trigger;

    ScheduledFuture scheduledTask = null;

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

        long startTime = stats.getStartTime() != null ? stats.getStartTime(): System.currentTimeMillis();
        long finishTime = stats.getFinishTime() != null ? stats.getFinishTime() : 0;

        ctx.update(new Date(startTime), new Date(startTime), new Date(finishTime));
        return trigger.nextExecutionTime(ctx);
    }

    @Override
    public synchronized void reloadAndRestart(boolean allowStartNow) {

        Settings configProps = configService.getSettings();
        if (configProps == null) {
            return;
        }

        /*
         * Cancel the scheduled task.
         */
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }

        /**
         * Cancels all tasks and waits for them to complete.
         */
        importTaskService.cancelAllTasks();

        /**
         * Add in the new scheduled task.
         */
        if (configProps.getSchedule() != null) {
            logger.info("Creating schedule using: {}", configProps.getSchedule());
            trigger = new CronTrigger(configProps.getSchedule());
            scheduledTask = cronScheduler.schedule(()-> {
                // Skip if its already running.
                if (!importTaskService.isImportTaskRunning()) {
                    importTaskService.submitImportTask(false);
                }
            }, trigger);
        }

        /**
         * Start a task if startNow is selected.
         */
        if (allowStartNow && configProps.isStartNow()) {
            logger.info("Start now selected, starting");
            cronScheduler.submit(()->
                    importTaskService.submitImportTask(false));
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
