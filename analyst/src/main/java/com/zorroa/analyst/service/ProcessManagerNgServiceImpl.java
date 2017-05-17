package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.analyst.Application;
import com.zorroa.analyst.cluster.ClusterProcess;
import com.zorroa.common.cluster.client.ClusterException;
import com.zorroa.common.cluster.client.MasterServerClient;
import com.zorroa.common.cluster.thrift.*;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.processor.Reaction;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.MetaZpsExecutor;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by chambers on 5/5/17.
 */
@Component
public class ProcessManagerNgServiceImpl  extends AbstractScheduledService
        implements ApplicationListener<ContextRefreshedEvent>, ProcessManagerNgService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessManagerNgServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    ThreadPoolExecutor analyzeExecutor;

    /**
     * Executor for handling task manipulation commands that
     * could encounter blocking IO. (hung mounts, etc). We don't want to block
     * thrift threads with these commands.
     */
    private final ExecutorService asyncCommandExecutor = Executors.newSingleThreadExecutor();


    @Value("${analyst.executor.enabled}")
    boolean executeEnabled;

    private final ConcurrentMap<Integer, ClusterProcess> processMap = Maps.newConcurrentMap();

    private List<String> hostList;

    private long hostListLoadedTime = 0;

    @Override
    public List<Integer> getTaskIds() {
        return ImmutableList.copyOf(processMap.keySet());
    }

    @Override
    public ClusterProcess queueClusterTask(TaskStartT task) {
        ClusterProcess process = new ClusterProcess(task);
        if (processMap.putIfAbsent(task.getId(), process) != null) {
            logger.warn("The task {} is already queued or executing.", task);
            throw new ClusterException("The task is already queued or executing.");
        }
        analyzeExecutor.submit(()->runClusterProcess(process));
        return process;
    }

    @Override
    public TaskResultT executeClusterTask(TaskStartT task) throws IOException {
        ClusterProcess p = new ClusterProcess(task);
        return runClusterProcess(p);
    }

    @Override
    public void kill(TaskKillT kill) {
        asyncCommandExecutor.execute(() -> {

            ClusterProcess p = processMap.get(kill.getId());
            if (p == null) {
                logger.warn("The task {} was not queued or executing, {}", kill);
                /**
                 * TODO: The task might be in-flight, need to keep a record of it.
                 * and kill it when it comes in.
                 */
                return;
            }

            MetaZpsExecutor mze = p.getZpsExecutor();
            if (mze.cancel()) {
                logger.info("The task {} was killed by:{}, reason: {}",
                        kill.getId(), kill.getUser(), kill.getReason());

                try {
                    Files.write(Paths.get(p.getTask().getLogPath()), ImmutableList.of("Process killed, reason: " + kill.getReason()),
                            StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void killAllTasks() {

    }

    private TaskResultT runClusterProcess(ClusterProcess proc) throws IOException {
        TaskStartT task = proc.getTask();
        AtomicReference<TaskResultT> result = new AtomicReference<>(new TaskResultT());
        File tmpScript = null;
        int exitStatus = -1;

        try {

            if (!passesPreflightChecks(proc)) {
                return null;
            }

            tmpScript = saveTempZpsScript(task);
            createLogDirectory(task);

            ZpsTask zpsTask = new ZpsTask();
            zpsTask.setId(task.getId());
            zpsTask.setArgs(Json.deserialize(task.getArgMap(), Json.GENERIC_MAP));
            zpsTask.setEnv(task.getEnv());
            zpsTask.setLogPath(task.getLogPath());
            zpsTask.setScriptPath(task.getScriptPath());

            MetaZpsExecutor zps = new MetaZpsExecutor(zpsTask,
                    new SharedData(task.getSharedDir()));
            zps.addReactionHandler((zpsTask1, sharedData, reaction) -> {
                if (reaction.getResponse() != null) {
                    result.get().setResult(Json.serialize(reaction.getResponse()));
                } else {
                    handleZpsReaction(zpsTask1, sharedData, reaction);
                }
            });

            proc.setZpsExecutor(zps);
            if (!passesPreflightChecks(proc)) {
                return null;
            }

            if (task.getId() > 0) {
                proc.getClient().reportTaskStarted(task.getId());
            }
            exitStatus = zps.execute();
            proc.setExitStatus(exitStatus);

        } finally {
            if (tmpScript != null) {
                tmpScript.delete();
            }
            // interactive tasks are not in the process map.
            if (processMap.remove(task.getId()) != null && task.getId() > 0) {
                TaskStopT stop = new TaskStopT();
                stop.setExitStatus(exitStatus);
                proc.getClient().reportTaskStopped(task.getId(), stop);
                proc.getClient().close();
            }
        }

        return result.get();
    }

    private void createLogDirectory(TaskStartT task) {
        File logPath = new File(task.getWorkDir() + "/logs");
        if (logPath.exists()) {
            return;
        }
        try {
            logPath.mkdirs();
        } catch (Exception e) {
            logger.warn("Unable to make log directory '{}'", task.getWorkDir(), e.getMessage());
            return;
        }
    }

    private void handleZpsReaction(ZpsTask zpsTask, SharedData sharedData, Reaction reaction) {
        ClusterProcess process = processMap.get(zpsTask.getId());
        MasterServerClient client = process.getClient();

        if (process.isKilled()) {
            return;
        }

        if (reaction.getExpand() != null) {
            ZpsScript script = reaction.getExpand();
            ExpandT expand = new ExpandT();
            expand.setScript(Json.serialize(script));
            expand.setName(script.getName());
            if (!Application.isUnitTest()) {
                client.expand(process.getId(), expand);
            }
            else {
                logger.info("Reacted with expand: {}", reaction.getExpand());
            }
        }

        if (reaction.getStats() != null) {
            Reaction.TaskStats stats = reaction.getStats();
            if (!Application.isUnitTest()) {
                client.reportTaskStats(process.getId(), new TaskStatsT()
                        .setErrorCount(stats.getErrorCount())
                        .setSuccessCount(stats.getSuccessCount())
                        .setWarningCount(stats.getWarningCount()));
            }
            else {
                logger.info("Reacted with stats: {}", reaction.getStats());
            }
        }

    }

    private File saveTempZpsScript(TaskStartT task) throws IOException {
        if (task.getScriptPath() != null) {
            return null;
        }

        File temp = File.createTempFile(UUID.randomUUID().toString(), ".json");
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(task.getScript());
            fos.close();
            task.setScriptPath(temp.toString());
        }
        return temp;
    }

    private boolean passesPreflightChecks(ClusterProcess proc) {
        if (proc.isKilled()) {
            logger.warn("Task {} did not pass pre-flight check, was killed", proc.getId());
            return false;
        }

        if (proc.getExitStatus() != -1) {
            logger.warn("Task {} did not pass pre-flight check, had exit status", proc.getId());
        }

        return true;
    }

    public synchronized void syncHostList() {
        if (System.currentTimeMillis() - hostListLoadedTime > 5000) {
            List<String> hosts = properties.getList("analyst.master.host");
            Collections.shuffle(hosts);
            hostList = ImmutableList.copyOf(hosts);
            hostListLoadedTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void runOneIteration() throws Exception {
        if (Application.isUnitTest()) {
            return;
        }

        if (analyzeExecutor.isShutdown()) {
            return;
        }

        int threads = properties.getInt("analyst.executor.threads");
        if (analyzeExecutor.getActiveCount() >= threads) {
            return;
        }

        try {
            syncHostList();
            for (String url: hostList) {

                String addr = MasterServerClient.convertUriToClusterAddr(url);
                MasterServerClient client = new MasterServerClient(addr);
                List<TaskStartT> tasks = Lists.newArrayList();
                try {
                    tasks = client.queuePendingTasks(addr, threads - analyzeExecutor.getActiveCount());
                } catch (Exception e) {
                    logger.warn("Unable to contact {} for scheduling op, {}", url, e.getMessage());
                }
                finally {
                    client.close();
                }

                for (TaskStartT task: tasks) {
                    queueClusterTask(task);
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to determine Archivist host list, {}", e.getMessage());
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(5, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (executeEnabled) {
            startAsync();
        }
    }

}
