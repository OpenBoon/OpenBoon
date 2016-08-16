package com.zorroa.archivist.service;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.AnalystClient;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.domain.TaskState;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JobExecutorService is responsible for pulling tasks out of elastic and
 * scheduling them onto analysts.
 *
 * Process constists of:
 *
 * 1. Pull list of analysts with space in their run queue.
 * 2. Pulling list of N tasks from the tasks table in the waiting state.
 * 3. Queue tasks to analyst, updating the task so its not requeued.
 * 4. Handle cases where analysts go down and lose tasks by requeuing them.
 */
@Service
public class JobExecutorServiceImpl extends AbstractScheduledService implements JobExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(JobExecutorServiceImpl.class);

    @Autowired
    AnalystService analystService;

    @Autowired
    TaskDao taskDao;

    @Autowired
    AssetService assetService;

    @Autowired
    JobService jobService;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    SharedData sharedData;

    @Autowired
    UserDao userDao;

    /**
     * Will be true if the scheduler is working.
     */
    private final AtomicBoolean beingScheduled = new AtomicBoolean(false);

    /**
     * A place to queue up requests to run the scheduler when a job
     * is expanded.  Never run the scheduler from more than 1 thread.
     */
    private final ExecutorService scheduleNow = Executors.newSingleThreadExecutor();

    /**
     * A thread pool for sending out tasks so we don't block the scheduler
     * on network I/O.
     */
    private final ExecutorService dispatchQueue = Executors.newFixedThreadPool(4);

    @PostConstruct
    public void init() {
        if (!ArchivistConfiguration.unittest) {
            startAsync();
        }
    }

    @Override
    protected void runOneIteration() throws Exception {
        schedule();
        checkForExpired();
    }

    @Override
    public void expand(ZpsScript script) {

        if (logger.isDebugEnabled()) {
            logger.debug("Expanding: {}", Json.prettyString(script));
        }

        jobService.createTask(script);
        if (!beingScheduled.get()) {
            scheduleNow.execute(() -> schedule());
        }
    }

    @Override
    public void schedule() {

        if (!beingScheduled.compareAndSet(false, true)) {
            return;
        }

        try {
            if (ArchivistConfiguration.unittest) {
                unittestSchedule();
            } else {
                AnalystClient analysts = analystService.getAnalystClient();
                if (!analysts.getLoadBalancer().hasHosts()) {
                    logger.debug("No analysts available for running tasks.");
                    return;
                }

                for (ZpsScript task : taskDao.getWaiting(10)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Starting: {}", Json.prettyString(task));
                    }
                    if (jobService.setTaskQueued(task)) {
                        dispatchQueue.execute(()-> {
                            try {
                                analysts.execute(task);
                                HttpHost host = analysts.getLoadBalancer().lastHost();
                                if (host != null) {
                                    jobService.setHost(task, host.getHostName());
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to dispatch task: ", e);
                                jobService.setTaskState(task, TaskState.Waiting, TaskState.Queued);
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            // don't let this function exit with an exception.
            logger.warn("Failed to run schedule,", e);
        }
        finally {
            logger.debug("scheduling finished.");
            beingScheduled.set(false);
        }
    }

    /**
     * Called by unit tests.
     */
    public void unittestSchedule() {
        for (ZpsScript task: taskDao.getWaiting(10)) {
            logger.debug("SCHEDULE");
            logger.debug("{}", Json.prettyString(task));
            logger.debug("SCHEDULE");

            if (!jobService.setTaskState(task, TaskState.Queued, TaskState.Waiting)) {
                throw new RuntimeException("Failed to queue task");
            }

            if (!jobService.setTaskState(task, TaskState.Running, TaskState.Queued)) {
                logger.warn("Failed to set task running: {}", task);
                throw new RuntimeException("Failed to run task");
            }
        }
    }

    /**
     * Look for tasks that have been queued or running for 30 minutes and reset them
     * back to waiting.
     *
     * TODO: may need to verify with analyst that its still around.
     */
    public void checkForExpired() {
        List<ZpsTask> expired = taskDao.getOrphanTasks(10, 30, TimeUnit.MINUTES);
        if (!expired.isEmpty()) {
            logger.warn("Found {} expired tasks!", expired.size());
            for (ZpsTask task : expired) {
                logger.warn("resetting task {} to Waiting", task.getTaskId());
                if (!jobService.setTaskState(task, TaskState.Waiting, TaskState.Queued)) {
                    jobService.setTaskState(task, TaskState.Waiting, TaskState.Running);
                }
            }
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(10, 5, TimeUnit.SECONDS);
    }
}
