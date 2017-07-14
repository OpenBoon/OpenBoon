package com.zorroa.analyst.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.analyst.Application;
import com.zorroa.analyst.cluster.ClusterProcess;
import com.zorroa.common.cluster.client.ClusterConnectionException;
import com.zorroa.common.cluster.client.ClusterException;
import com.zorroa.common.cluster.client.MasterServerClient;
import com.zorroa.common.cluster.thrift.*;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.config.NetworkEnvironment;
import com.zorroa.sdk.processor.Reaction;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.MetaZpsExecutor;
import com.zorroa.sdk.zps.ZpsError;
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

    @Autowired
    NetworkEnvironment networkEnvironment;

    /**
     * Executor for handling task manipulation commands that
     * could encounter blocking IO. (hung mounts, etc). We don't want to block
     * thrift threads with these commands.
     */
    private final ExecutorService asyncCommandExecutor = Executors.newSingleThreadExecutor();

    /**
     * The amount of time between polling the archivist.
     */
    private static final long POLL_TIME_MS = 5000;

    @Value("${analyst.executor.enabled}")
    boolean executeEnabled;

    private volatile ConcurrentMap<Integer, ClusterProcess> processMap = Maps.newConcurrentMap();

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
        logger.info("Submitting task to execute: {}, total tasks {}", task.getId(), processMap.size());
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

                /**
                 * If the task is interactive, then response and errors
                 * are handled here.
                 */
                if (task.getId() == 0) {
                    if (reaction.getResponse() != null) {
                        result.get().setResult(Json.serialize(reaction.getResponse()));
                    }
                    else if (reaction.getError() != null) {
                        result.get().addToErrors(newTaskError(reaction.getError()));
                    }
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

        /**
         * TODO: queue up a bunch here.
         */
        if (reaction.getError() != null) {
            client.reportTaskErrors(zpsTask.getId(), ImmutableList.of(
                    newTaskError(reaction.getError())));
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

    /**
     * Convert the ZpsError into a TaskErrorT that can be
     * sent to the archivist.
     *
     * @param zpsError
     * @return
     */
    private TaskErrorT newTaskError(ZpsError zpsError) {
        TaskErrorT error = new TaskErrorT();
        error.setMessage(zpsError.getMessage());
        error.setPhase(zpsError.getPhase());
        error.setProcessor(zpsError.getProcessor());
        error.setSkipped(zpsError.isSkipped());
        error.setTimestamp(System.currentTimeMillis());

        error.setStack(ImmutableList.of(new StackElementT()
                .setClassName(zpsError.getClassName())
                .setFile(zpsError.getFile())
                .setLineNumber(zpsError.getLineNumber())
                .setMethod(zpsError.getMethod())));

        if (zpsError.getOrigin() != null) {
            error.setId(zpsError.getId());
            error.setOriginService(zpsError.getOrigin().getService());
            error.setOriginPath(zpsError.getOrigin().getPath());
            error.setPath(zpsError.getPath());
        }
        return error;
    }

    private static final class Connection {
        public MasterServerClient client;
        public long backoffTill = 0;

        public Connection(MasterServerClient client) {
            this.client = client;
        }
        public void backoff() {
            client.close();
            backoffTill = System.currentTimeMillis() + (60 * 1000);
        }
    }

    private final LoadingCache<String, Connection> connectioncCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .initialCapacity(100)
            .concurrencyLevel(1)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, Connection>() {
                public Connection load(String addr) throws Exception {
                    MasterServerClient client = new MasterServerClient(addr);
                    client.setMaxRetries(0);
                    client.setSocketTimeout(2000);
                    client.setConnectTimeout(1000);
                    return new Connection(client);

                }
            });

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
                Connection conn = connectioncCache.get(addr);

                if (conn.backoffTill > System.currentTimeMillis()) {
                    continue;
                }

                try {
                    List<TaskStartT> tasks = conn.client.queuePendingTasks(
                            networkEnvironment.getClusterAddr(), threads - analyzeExecutor.getActiveCount());

                    if (!tasks.isEmpty()) {
                        logger.info("Obtained {} tasks from {}", tasks.size(), addr);
                    }

                    for (TaskStartT task: tasks) {
                        queueClusterTask(task);
                    }
                } catch (ClusterConnectionException e) {
                    conn.backoff();
                    // ignore this, host is probably down.
                }
                catch (Exception e) {
                    conn.backoff();
                    logger.warn("Failed to contact {} for scheduling op, unexpected {}",
                            addr, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to determine Archivist host list, {}", e.getMessage());
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(
                POLL_TIME_MS, POLL_TIME_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (executeEnabled) {
            startAsync();
        }
    }

}
