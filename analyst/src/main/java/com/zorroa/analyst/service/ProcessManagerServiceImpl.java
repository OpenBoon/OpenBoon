package com.zorroa.analyst.service;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.io.LineReader;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.analyst.AnalystProcess;
import com.zorroa.analyst.Application;
import com.zorroa.analyst.ArchivistClient;
import com.zorroa.common.cluster.ClusterException;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.*;
import com.zorroa.sdk.processor.Reaction;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
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
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


/**
 * Created by chambers on 2/8/16.
 */
@Component
public class ProcessManagerServiceImpl extends AbstractScheduledService
        implements ProcessManagerService, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ProcessManagerServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    ThreadPoolExecutor analyzeExecutor;

    @Value("${analyst.executor.enabled}")
    boolean executeEnabled;

    /**
     * Before a process is started or stopped, the process lock must be obtained
     * so some checks can be made if the process can be started or not.
     */
    private final Object processLock = new Object();

    /**
     * Executor for handling task manipulation commands.
     */
    private final ExecutorService asyncCommandExecutor = Executors.newSingleThreadExecutor();

    /**
     * Maintains a list of running processes.
     */
    private final ConcurrentMap<Integer, AnalystProcess> processMap = Maps.newConcurrentMap();

    private List<String> hostList;

    private long hostListLoadedTime = 0;

    @Override
    public Collection<AnalystProcess> getProcesses() {
        return processMap.values();
    }

    @Override
    public List<Integer> getTaskIds() {
        return ImmutableList.copyOf(processMap.keySet());
    }

    @Override
    public boolean stopTask(AnalystProcess p, TaskState newState, String reason) {
        boolean processKilled = false;
        Stopwatch timer = Stopwatch.createStarted();

        try {
            while(true) {
                // If the process is null, the process never started
                if (p.getProcess() != null) {
                    for (int i = 0; i < 5; i++) {
                        processKilled = p.getProcess().destroyForcibly().waitFor(5, TimeUnit.SECONDS);
                        if (processKilled) {
                            logger.info("Process {} was killed, try #{}", p.getTask().getTaskId(), i+1);
                            p.setNewState(newState);
                            break;
                        } else {
                            logger.warn("Failed to kill process: {}, try #{}", p.getTask().getTaskId(), i+1);
                        }
                    }

                    if (p.getLogFile() != null) {
                        if (processKilled) {
                            Files.write(p.getLogFile(), ImmutableList.of("Process killed, reason: " + reason),
                                    StandardOpenOption.APPEND);
                        }
                        else {
                            Files.write(p.getLogFile(), ImmutableList.of("Attempt to kill process failed."),
                                    StandardOpenOption.APPEND);
                        }
                    }
                    // Break out of infinite loop.
                    break;
                } else {

                    if (p.getNewState() != null) {
                        // Another thread has set a new state
                        logger.warn("The task {} had it's new state set by another thread.", p.getTask());
                        break;
                    }

                    if (!processMap.containsKey(p.getTask().getTaskId())) {
                        logger.warn("Something else kill the running task: {}", p.getTask());
                        break;
                    }

                    if (timer.elapsed(TimeUnit.SECONDS) >= 60) {
                        logger.warn("Waited 60 seconds for task {} to start, never started", p.getTask());
                        break;
                    }

                    logger.warn("The task: {} does not having a process yet, waiting.", p.getTask());
                    /*
                     * The process has not started yet, we'll wait till it starts to kill it.
                     * This should fix a race condition where if you kill a task when no process, the process
                     * will run anyway.
                     */
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to kill task {}", p.getTask(), e);
        }

        return processKilled;
    }

    @Override
    public void asyncStopTask(ExecuteTaskStop task) {
        asyncCommandExecutor.execute(() -> {
            synchronized (processLock) {
                AnalystProcess p = processMap.get(task.getTaskId());
                if (p == null) {
                    logger.warn("Attmepted to stop non-existing process for task: {}", task);
                }
                else {
                    stopTask(p, task.getNewState(), task.getReason());
                }
            }
        });
    }

    @Override
    public void stopAllTasks() {
        synchronized (processLock) {
            /**
             * Shutdown the thread pool so no more tasks can get queued.
             */
            analyzeExecutor.shutdown();

            for (Map.Entry<Integer, AnalystProcess> entry : processMap.entrySet()) {
                stopTask(entry.getValue(), TaskState.Waiting, "All tasks killed by server shutdown");
            }
        }
    }

    @Override
    public Future<AnalystProcess> execute(ExecuteTaskStart task, boolean async) {

        /**
         * The processes is added to the map first, this is because we also keep track
         * of queued processes.
         */
        AnalystProcess p = processMap.putIfAbsent(
                task.getTask().getTaskId(), new AnalystProcess(task));

        if (p != null) {
            /**
             * Not sure if we should return null here, or an exception.  Nothing gets
             * thrown back to the archivist, it doesn't care if the task doesn't execute.
             */
            logger.warn("The task {} is already queued or executing.", task.getTaskId());
            throw new ClusterException("The task is already queued or executing.");
        }

        if (async) {
            try {
                return analyzeExecutor.submit(() -> execute(task));
            } catch (RejectedExecutionException ex){
                processMap.remove(task.getTaskId());
                throw new ClusterException("Failed to queue task for execution, " + ex.getMessage());
            }
        }
        else {
            final AnalystProcess result = execute(task);
            return new Future<AnalystProcess>() {

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public AnalystProcess get() throws InterruptedException, ExecutionException {
                    return result;
                }

                @Override
                public AnalystProcess get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return result;
                }
            };
        }
    }

    private boolean isTaskValid(AnalystProcess proc) {
        if (analyzeExecutor.isShutdown()) {
            logger.warn("The analyst is shutting down and longer excepting tasks, {}", proc.getTask());
            return false;
        }

        if (proc == null) {
            logger.warn("No process record exists for task: {}", proc.getTask());
            return false;
        }

        return true;
    }

    private AnalystProcess execute(ExecuteTaskStart task) {
        int taskId = task.getTask().getTaskId();

        AnalystProcess proc = processMap.get(task.getTask().getTaskId());
        try {
            if (!isTaskValid(proc)) {
                return proc;
            }

            logger.info("loading ZPS script : {}", task.getScriptPath());
            ZpsScript script;
            try {
                script = Json.Mapper.readValue(new File(task.getScriptPath()), ZpsScript.class);
            } catch (IOException e) {
                throw new ClusterException("Invalid ZPS script, " + e, e);
            }

            SharedData shared = new SharedData(task.getSharedPath());
            task.putToEnv("ZORROA_CLUSTER_PATH_SHARED", shared.getRoot().toString());
            task.putToEnv("ZORROA_CLUSTER_PATH_OFS", shared.resolvestr("ofs"));
            task.putToEnv("ZORROA_CLUSTER_PATH_PLUGINS", shared.resolvestr("plugins"));
            task.putToEnv("ZORROA_CLUSTER_PATH_MODELS", shared.resolvestr("models"));
            task.putToEnv("ZORROA_ARCHIVIST_URL", task.getArchivistHost());

            int exit = 1;
            Stopwatch timer = Stopwatch.createStarted();
            try {
                if (!Application.isUnitTest()) {
                    proc.getClient().reportTaskStarted(new ExecuteTaskStarted(task.getTask()));
                }
                String scriptPath = task.getScriptPath();
                for (; ; ) {
                    String lang = determineLanguagePlugin(script);

                    if (!isTaskValid(proc)) {
                        return proc;
                    }

                    logger.debug("running script with language: {}", lang);
                    String[] command = createCommand(shared, task, scriptPath, lang);
                    exit = runProcess(command, task, proc);

                    if (exit != 0) {
                        break;
                    }

                    ZpsScript next = proc.nextProcess();
                    if (next == null) {
                        break;
                    }

                    script = next;
                    proc.incrementProcessCount();
                    scriptPath = task.getScriptPath() + "." + proc.getProcessCount();

                    logger.info("Writing next script {}", scriptPath);
                    Files.deleteIfExists(Paths.get(scriptPath));
                    Json.Mapper.writeValue(new File(scriptPath), script);
                }

            } catch (Exception e) {
                // don't throw anything, just log
                logger.warn("Failed to execute process: ", e);
                exit = 1;
            } finally {
                logger.info("Task {} stopped, exit status: {} in {}ms", taskId, exit,
                        timer.stop().elapsed(TimeUnit.MILLISECONDS));

                /**
                 * Set the new task state based on the exit value of the process.
                 */
                TaskState newState = exit == 0 ? TaskState.Success : TaskState.Failure;

                /**
                 * If the running process has a new state attached, use that instead.
                 */

                if (proc.getNewState() == null) {
                    logger.info("Setting newstate of finished task to: {}", newState);
                    proc.setNewState(newState);
                }


                if (logger.isDebugEnabled()) {
                    logger.debug("Completed {}", Json.prettyString(script));
                }

                if (!Application.isUnitTest()) {
                    proc.getClient().reportTaskStopped(new ExecuteTaskStopped(task.getTask())
                            .setNewState(proc.getNewState()));
                }
            }
        } finally {
            /**
             * If this process returns in any way, the process has to be removed from process map.
             */
            processMap.remove(task.getTask().getTaskId());
        }
        return proc;
    }

    public String[] createCommand(SharedData shared, ExecuteTaskStart task, String scriptPath, String lang) throws IOException {
        String rootPath = shared.getRoot().toString();
        ImmutableList.Builder<String> b = ImmutableList.<String>builder()
                .add(String.format("%s/plugins/lang-%s/bin/zpsgo", rootPath, lang))
                .add("--shared-path", rootPath)
                .add("--script", scriptPath);

        if (task.getArgs() != null) {
            task.getArgs().forEach((k, v) -> {
                b.add("--global", k.concat("=").concat(v.toString()));
            });
        }
        return b.build().toArray(new String[] {});
    }

    public String determineLanguagePlugin(ZpsScript script) {
        if (script.getGenerate() != null && !script.getGenerate().isEmpty()) {
            return script.getGenerate().get(0).getLanguage();
        }
        else if (script.getExecute()!= null && !script.getExecute().isEmpty()) {
            return script.getExecute().get(0).getLanguage();
        }
        else {
            return "java";
        }
    }

    private static final int NEWLINE = '\n';

    public ProcessBuilder makeProcess(String[] command, ExecuteTaskStart task, AnalystProcess proc) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Map<String, String> env = builder.environment();

        Map<String, Object> clusterEnv = properties.getMap("zorroa.cluster.env");
        if (clusterEnv != null) {
            for (Map.Entry<String,Object> e: clusterEnv.entrySet()) {
                env.put(e.getKey(), (String)e.getValue());
            }
        }

        if (task.getEnv() != null) {
            for (Map.Entry<String, String> e: task.getEnv().entrySet()) {
                env.put(e.getKey(), e.getValue());
            }
        }
        return builder;
    }

    public int runProcess(String[] command, ExecuteTaskStart task, AnalystProcess proc) throws IOException {

        int exit = 1;
        Process process = null;
        FileOutputStream logStream = null;
        String error = null;
        Stopwatch timer = Stopwatch.createStarted();

        /**
         * Synchronized around the cached Process object, if a new state has not been set
         *  (like cancel/killed, then the process can start). The Synchronized ensures
         *  that canceling the proc happens serially.
         */
        if (proc.getNewState() == null) {
            ProcessBuilder builder = makeProcess(command, task, proc);
            logger.info("running cmd: {}", String.join(" ", builder.command()));
            process = builder.start();
            proc.setProcess(process);
            proc.setLogFile(Paths.get(task.getLogPath()));
        } else {
            logger.warn("The task {} state was changed to: {}", task.getTask().getTaskId(),
                    proc.getNewState());
            return 13;
        }

        try {

            /**
             * Once the process is started, open the log file.
             */
            logStream = new FileOutputStream(new File(task.getLogPath()), proc.getProcessCount() > 1);
            LineReader reader = new LineReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder(1024 * 1024);
            boolean buffer = false;
            String line;

            for (Map.Entry<String, String> e : task.getEnv().entrySet()) {
                if (e.getKey().equals("ZORROA_HMAC_KEY")) {
                    continue;
                }
                logStream.write(new StringBuilder(256)
                        .append("ENV: ")
                        .append(e.getKey())
                        .append("=")
                        .append(e.getValue())
                        .toString().getBytes());
                logStream.write(NEWLINE);
            }

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(ZpsScript.SUFFIX)) {
                    /**
                     * The buffer cannot be parsed for some reason, we'll just fail the task.
                     */
                    try {
                        processBuffer(sb, task, logStream, proc);
                        buffer = false;
                        logStream.write(NEWLINE);
                    } catch (Exception e) {
                        logger.warn("Failed to process buffer {}, unexpected ", sb.toString(), e);
                        error = e.getMessage();
                        process.destroyForcibly();
                        break;
                    }
                } else if (buffer) {
                    sb.append(line);
                } else if (line.startsWith(ZpsScript.PREFIX)) {
                    buffer = true;
                    sb.setLength(0);
                } else {
                    logStream.write(line.getBytes());
                    logStream.write(NEWLINE);
                    logStream.flush();
                }
            }

        } catch (Exception e) {
            logger.warn("Unexpected error while running process {}, {}", task.getTaskId(),
                    task.getScriptPath());
            error = e.getMessage();
            if (process != null) {
                // Make sure to kill process just in case
                process.destroyForcibly();
            }
        }
        finally {

            if (process != null) {
                try {
                    exit = process.waitFor();
                } catch (InterruptedException e) {
                    logger.warn("Process interrupted: ", e);
                }
            }
            if (logStream != null) {
                try {
                    writeFooter(timer, task, logStream, error, exit);
                } catch (Exception e) {
                    logger.warn("Error writing footer, ", e);
                }
                Closeables.close(logStream, true);
            }
        }

        return exit;
    }

    public void writeFooter(Stopwatch timer, ExecuteTaskStart task, FileOutputStream logStream, String error, int exit) throws IOException {
        logStream.write("##################################\n".getBytes());
        logStream.write(String.format("Duration: %.2f minutes\n", timer.elapsed(TimeUnit.SECONDS) / 60.0).getBytes());
        logStream.write(String.format("Log: %s\n", task.getLogPath()).getBytes());
        logStream.write(String.format("Script: %s\n", task.getScriptPath()).getBytes());
        logStream.write(String.format("Exit Status: %d\n", exit).getBytes());
        if (error != null) {
            logStream.write(String.format("Error: %s\n", error).getBytes());
        }
        logStream.write("##################################\n".getBytes());
        logStream.flush();
    }

    public void processBuffer(StringBuilder sb, ExecuteTaskStart start, FileOutputStream log, AnalystProcess process) throws IOException {
        String scriptText = sb.toString();

        /**
         * Parse the string into a Reaction.  If it doesn't parse, an exception is thrown
         * out to the I/O loop, which is handled there.
         */
        Reaction reaction = Json.deserialize(scriptText, Reaction.class);

        if (reaction.getNextProcess() != null) {
            logger.info("Adding process to queue for job: {}", start.getTask().getJobId());
            process.addToNextProcess(reaction.getNextProcess());
        }

        if (reaction.getExpand() != null) {
            logger.info("Processing expand from job: {}", start.getTask().getJobId());
            ZpsScript script = reaction.getExpand();

            log.write(String.format("Expanding with %d tasks\n", script.getOver().size()).getBytes());

            ExecuteTaskExpand st = new ExecuteTaskExpand();
            st.setScript(Json.serializeToString(script));
            st.setParentTaskId(start.getTask().getTaskId());
            st.setJobId(start.getTask().getJobId());
            st.setName(script.getName());
            process.getClient().expand(st);
        }

        if (reaction.getResponse() != null) {
            logger.info("Processing response from task: {}", Json.serializeToString(start.getTask()));
            ExecuteTaskResponse rsp = new ExecuteTaskResponse(start.getTask(), reaction.getResponse());
            process.getClient().respond(rsp);
        }

        if (reaction.getStats() != null) {
            process.getClient().reportTaskStats(new ExecuteTaskStats(start.getTask())
                    .setErrorCount(reaction.getStats().getErrorCount())
                    .setSuccessCount(reaction.getStats().getSuccessCount())
                    .setWarningCount(reaction.getStats().getWarningCount()));
            if (reaction.getStats().getSkipped()!= null) {
                for (String path: reaction.getStats().getSkipped()) {
                    logger.info("SKIPPED:{}", path);
                }
            }

            if (reaction.getStats().getErrored()!= null) {
                for (String path : reaction.getStats().getErrored()) {
                    logger.info("ERROR:{}", path);
                }
            }
        }
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
                try {
                    ArchivistClient client = new ArchivistClient(url);
                    List<ExecuteTaskStart> tasks = client.queueNextTasks(new ExecuteTaskRequest()
                            .setId(System.getProperty("analyst.id"))
                            .setUrl(System.getProperty("server.url"))
                            .setCount(threads - analyzeExecutor.getActiveCount()));

                    if (!tasks.isEmpty()) {
                        for (ExecuteTaskStart task : tasks) {
                            try {
                                execute(task, true);
                            } catch (Exception e) {
                                logger.warn("Failed to queue task: {}", task, e);
                                client.rejectTask(new ExecuteTaskStopped(
                                        task.getTask(), TaskState.Waiting));
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Unable to contact Archivist for scheduling op, {}", e.getMessage());
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
