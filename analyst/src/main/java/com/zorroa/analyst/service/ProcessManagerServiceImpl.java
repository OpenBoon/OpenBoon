package com.zorroa.analyst.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.zorroa.analyst.AnalystProcess;
import com.zorroa.analyst.Application;
import com.zorroa.analyst.ArchivistClient;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.*;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.processor.Reaction;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.*;


/**
 * Created by chambers on 2/8/16.
 */
@Component
public class ProcessManagerServiceImpl implements ProcessManagerService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessManagerServiceImpl.class);

    @Autowired
    AssetDao assetDao;

    @Autowired
    ArchivistClient archivistClient;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    ListeningExecutorService analyzeExecutor;

    /**
     * Executor for handling task manipulation commands.
     */
    private final ExecutorService asyncCommandExecutor = Executors.newSingleThreadExecutor();

    /**
     * Maintains a list of running processes.
     */
    private final ConcurrentMap<Integer, AnalystProcess> processMap = Maps.newConcurrentMap();

    @Override
    public boolean stopTask(ExecuteTaskStop stop) {
        int taskId = stop.getTask().getTaskId();
        /**
         * Set to true if we actually kill the process.
         */
        boolean result = false;
        AnalystProcess p = processMap.remove(stop.getTask().getTaskId());

        if (p == null) {
            logger.warn("Attempted to stop task {}, was not queued or running.", taskId);
        }
        else {
            try {
                synchronized (p) {
                    p.setNewState(stop.getNewState());
                    // If the process is null, the process never started
                    if (p.getProcess() != null) {
                        p.getProcess().destroyForcibly().waitFor();
                        result = true;
                    } else {
                        logger.warn("The process for task {} never started.", taskId);
                    }
                }

                if (p.getLogFile() != null) {
                    Files.write(p.getLogFile(), ImmutableList.of("Process killed, reason: " + stop.getReason()),
                            StandardOpenOption.APPEND);
                }

            } catch (Exception e) {
                logger.warn("Failed to kill task {}", taskId, e);
                /**
                 * In this case, we don't report the task as stopped.
                 */
                return false;
            }
        }

        /**
         * Report the task as stopped, even if we didn't stop the task.
         */
        if (!Application.isUnitTest()) {
            archivistClient.reportTaskStopped(new ExecuteTaskStopped(stop.getTask())
                    .setNewState(stop.getNewState()));
        }
        return result;
    }

    @Override
    public void asyncStopTask(ExecuteTaskStop task) {
        asyncCommandExecutor.execute(() -> {
            stopTask(task);
        });
    }

    @Override
    public Future<AnalystProcess> execute(ExecuteTaskStart task, boolean async) {
        AnalystProcess p = processMap.putIfAbsent(task.getTask().getTaskId(), new AnalystProcess());
        if (p != null) {
            /**
             * Not sure if we should return null here, or an exception.  Nothing gets
             * thrown back to the archivist, it doesn't care if the task doesn't execute.
             */
            logger.warn("The task {} is already queued or executing.", task.getTaskId());
            return null;
        }

        if (async) {
            return analyzeExecutor.submit(() -> execute(task));
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

    private AnalystProcess execute(ExecuteTaskStart task) {
        Preconditions.checkNotNull(task.getTask().getTaskId(), "Task ID cannot be null");
        Preconditions.checkNotNull(task.getTask().getJobId(), "Job ID cannot be null");

        int taskId = task.getTask().getTaskId();

        AnalystProcess proc = processMap.get(task.getTask().getTaskId());
        if (proc == null) {
            logger.warn("No process record exists for task: {}", task);
            return proc;
        }

        if (proc.getNewState() != null) {
            logger.warn("A new state was already set for the task: {}", task);
            return proc;
        }

        ZpsScript script = Json.deserialize(task.getScript(), ZpsScript.class);
        task.putToEnv("ZORROA_ARCHIVIST_URL", properties.getString("analyst.master.host"));

        int exit = 1;
        Stopwatch timer = Stopwatch.createStarted();
        try {
            if (!Application.isUnitTest()) {
                archivistClient.reportTaskStarted(new ExecuteTaskStarted(task.getTask()));
            }
            String lang = determineLanguagePlugin(script);
            logger.debug("running script with language: {}", lang);
            String[] command = createCommand(script, task, lang);
            logger.info("running command: {}", String.join(" ", command));
            exit = runProcess(command, task, proc);

        } catch (Exception e) {
            // don't throw anything, just log
            logger.warn("Failed to execute process: ", e);
            exit=1;
        }
        finally {
            logger.info("Task {} stopped, exit status: {} in {}ms", taskId, exit,
                timer.stop().elapsed(TimeUnit.MILLISECONDS));

            /**
             * Set the new task state based on the exit value of the process.
             */
            TaskState newState = exit == 0 ? TaskState.Success : TaskState.Failure;

            /**
             * If the running proces has a new state attached, use that instead.
             */
            if (proc.getNewState() == null) {
                proc.setNewState(newState);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Completed {}", Json.prettyString(script));
            }

            if (!Application.isUnitTest()) {
                archivistClient.reportTaskStopped(new ExecuteTaskStopped(task.getTask())
                        .setNewState(proc.getNewState()));
            }
        }

        return proc;
    }

    public String[] createCommand(ZpsScript script, ExecuteTaskStart task, String lang) throws IOException {
        ImmutableList.Builder<String> b = ImmutableList.<String>builder()
                .add(String.format("%s/lang-%s/bin/zpsgo", properties.getString("zorroa.cluster.path.plugins"), lang))
                .add("-shared-path", properties.getString("zorroa.cluster.path.shared"))
                .add("-plugin-path", properties.getString("zorroa.cluster.path.plugins"))
                .add("-model-path", properties.getString("zorroa.cluster.path.models"))
                .add("-ofs-path", properties.getString("zorroa.cluster.path.ofs"))
                .add("-export-path", properties.getString("zorroa.cluster.path.exports"))
                .add("-script", writeScript(script).toString());

        if (task.getArgs() != null) {
            task.getArgs().forEach((k, v) -> {
                b.add("-global", k.concat("=").concat(v.toString()));
            });
        }
        return b.build().toArray(new String[] {});
    }

    public Path writeScript(ZpsScript script) throws IOException {
        Path path = Files.createTempFile("zorroa", "script");
        Json.Mapper.writeValue(path.toFile(), script);
        return path;
    }

    public String determineLanguagePlugin(ZpsScript script) {
        if (script.getExecute() != null && !script.getExecute().isEmpty()) {
            return script.getExecute().get(0).getLanguage();
        }
        else {
            return "java";
        }
    }

    private static final int NEWLINE = '\n';

    public int runProcess(String[] command, ExecuteTaskStart task, AnalystProcess proc) throws IOException {

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        if (task.getEnv() != null) {
            Map<String,String> env = builder.environment();
            for (Map.Entry<String, String> e: task.getEnv().entrySet()) {
                env.put(e.getKey(), e.getValue());
            }
        }

        Process process = builder.start();
        synchronized (proc) {
            if (proc.getNewState() == null) {
                proc.setProcess(process);
                proc.setLogFile(Paths.get(task.getLogPath()));
            }
            else {
                logger.warn("The task {} state was changed to: {}", task.getTask().getTaskId(),
                        proc.getNewState());
                return 1;
            }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder sb = null;
        boolean buffer = false;
        String line;

        try (FileOutputStream logStream = new FileOutputStream(new File(task.getLogPath()))) {
            for (Map.Entry<String, String> e: task.getEnv().entrySet()) {
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
                    processBuffer(sb, task, logStream);
                    logStream.write(NEWLINE);
                    buffer = false;
                    sb.setLength(0);
                } else if (buffer) {
                    sb.append(line);
                } else if (line.startsWith(ZpsScript.PREFIX)) {
                    buffer = true;
                    sb = new StringBuilder(8096);
                }
                else {
                    logStream.write(line.getBytes());
                    logStream.write(NEWLINE);
                }
            }
        }

        int exit;
        try {
            exit = process.waitFor();
        } catch (InterruptedException e) {
            logger.warn("Process interrupted: ", e);
            exit = 1;
        }
        return exit;
    }

    public void processBuffer(StringBuilder sb, ExecuteTaskStart start, FileOutputStream log) throws IOException {
        String scriptText = sb.toString();

        // Double check it can be serialized.
        Reaction reaction = Json.deserialize(scriptText, Reaction.class);

        if (reaction.getExpand() != null) {
            logger.info("Processing expand from job: {}", start.getTask().getJobId());
            ZpsScript script = reaction.getExpand();

            log.write(ZpsScript.PREFIX.getBytes());
            log.write(NEWLINE);
            log.write(Json.prettyString(script).getBytes());
            log.write(NEWLINE);
            log.write(ZpsScript.SUFFIX.getBytes());

            ExecuteTaskExpand st = new ExecuteTaskExpand();
            st.setScript(Json.serializeToString(script));
            st.setParentTaskId(start.getTask().getTaskId());
            st.setJobId(start.getTask().getJobId());
            st.setName(script.getName());
            archivistClient.expand(st);
        }

        if (reaction.getResponse() != null) {
            logger.info("Processing response from task: {}", Json.serializeToString(start.getTask()));
            ExecuteTaskResponse rsp = new ExecuteTaskResponse(start.getTask(), reaction.getResponse());
            archivistClient.respond(rsp);
        }

        if (reaction.getStats() != null) {
            archivistClient.reportTaskStats(new ExecuteTaskStats(start.getTask())
                    .setErrorCount(reaction.getStats().getErrorCount())
                    .setSuccessCount(reaction.getStats().getSuccessCount())
                    .setWarningCount(reaction.getStats().getWarningCount()));
        }
    }
}
