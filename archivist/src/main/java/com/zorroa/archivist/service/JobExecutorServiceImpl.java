package com.zorroa.archivist.service;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.AnalystClient;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.*;
import com.zorroa.common.repository.AnalystDao;
import com.zorroa.sdk.client.exception.ArchivistReadException;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.Json;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
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
@Component
public class JobExecutorServiceImpl extends AbstractScheduledService
        implements JobExecutorService, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(JobExecutorServiceImpl.class);

    @Autowired
    AnalystService analystService;

    @Autowired
    AnalystDao analystDao;

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

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!ArchivistConfiguration.unittest) {
            startAsync();
        }
    }

    @Override
    protected void runOneIteration() throws Exception {
        /**
         * Note that, if this function throws then scheduling will stop
         * so just in case we're not letting anything bubble up from here.
         */
        try {
            schedule();
            checkForExpired();
        } catch (Exception e) {
            logger.warn("Job executor failed to schedule tasks, ", e);
        }
    }

    @Override
    public void queueSchedule() {
        if (!beingScheduled.get()) {
            scheduleNow.execute(() -> schedule());
        }
    }

    @Override
    public void schedule() {

        if (!beingScheduled.compareAndSet(false, true)) {
            return;
        }

        int taskCount = 0;
        Stopwatch timer = Stopwatch.createStarted();
        try {
            if (ArchivistConfiguration.unittest) {
                unittestSchedule();
            } else {
                AnalystClient analysts = analystService.getAnalystClient();
                if (!analysts.getLoadBalancer().hasHosts()) {
                    logger.debug("No analysts available for running tasks.");
                    return;
                }

                for (ExecuteTaskStart task : taskDao.getWaiting(10)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Starting: {}", Json.prettyString(task));
                    }
                    if (jobService.setTaskQueued(task)) {
                        taskCount++;
                        dispatchQueue.execute(()-> {
                            try {
                                analysts.execute(task);
                                HttpHost host = analysts.getLoadBalancer().lastHost();
                                if (host != null) {
                                    jobService.setHost(task, host.toString());
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
            beingScheduled.set(false);
            if (taskCount > 0) {
                logger.info("scheduled {} tasks in {}ms", taskCount, timer.elapsed(TimeUnit.MILLISECONDS));
            }
        }
    }

    private final Cache<Integer, SynchronousQueue<Object>> returnQueue = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    @Override
    public void handleResponse(ExecuteTaskResponse response) {
        logger.info("Processing job response, id:{}, data:{}", response.getJobId(), response.getResponse());
        try {
            SynchronousQueue<Object> queue = returnQueue.asMap().get(response.getJobId());
            if (queue == null) {
                logger.warn("Synchronous queue expired for job: {}", response.getJobId());
                return;
            }
            queue.offer(response.getResponse(), 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Waiting thread disappeared for job response ID: {}",
                    response.getJobId(), e);
        }
    }

    @Override
    public Object waitOnResponse(Job job) {
        returnQueue.put(job.getId(), new SynchronousQueue<>());
        queueSchedule();

        /*
         * Wait for the job to complete and submit and object.
         */
        try {
            return returnQueue.asMap().get(job.getId()).poll(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ArchivistReadException("Failed waiting on response, ", e);
        }
    }

    /**
     * Called by unit tests.
     */
    public void unittestSchedule() {
        for (ExecuteTaskStart task: taskDao.getWaiting(10)) {
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
        jobService.updatePingTime(analystDao.getRunningTaskIds());
        List<Task> expired = taskDao.getOrphanTasks(1, 30, TimeUnit.MINUTES);
        if (!expired.isEmpty()) {
            logger.warn("Found {} expired tasks!", expired.size());
            for (Task task : expired) {
                logger.warn("resetting task {} to Waiting", task.getTaskId());
                jobService.setTaskState(task, TaskState.Waiting);
            }
        }
    }

    @Override
    @Async
    public void retryTask(Task task) {
        if (TaskState.requiresStop(task.getState())) {
            killRunningTaskOnAnalyst(task, TaskState.Waiting);
        }
        else {
            jobService.setTaskState(task, TaskState.Waiting);
        }
    }

    @Override
    @Async
    public void skipTask(Task task) {
        if (TaskState.requiresStop(task.getState())) {
            killRunningTaskOnAnalyst(task, TaskState.Skipped);
        }
        else {
            jobService.setTaskState(task, TaskState.Skipped);
        }
    }

    public void killRunningTaskOnAnalyst(Task task, TaskState newState) {
        AnalystClient client = analystService.getAnalystClient(task.getHost());
        try {
            logger.info("Killing runinng task: {}", task);
            client.stop(new ExecuteTaskStop(new ExecuteTask(task.getJobId(), task.getTaskId(), task.getParentTaskId()) )
                    .setNewState(newState)
                    .setReason("Stopped by " + SecurityUtils.getUsername()));
        } catch (Exception e) {
            logger.warn("Failed to kill running task an analyst {}", task.getHost());
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(10, 5, TimeUnit.SECONDS);
    }
}
