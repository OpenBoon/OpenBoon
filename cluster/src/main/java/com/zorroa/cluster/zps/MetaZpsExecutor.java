package com.zorroa.cluster.zps;

import com.google.common.base.Stopwatch;
import com.google.common.collect.*;
import com.google.common.io.Closeables;
import com.google.common.io.LineReader;
import com.zorroa.sdk.processor.Reaction;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsError;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Can execute multi-language ZPS script.
 */
public class MetaZpsExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MetaZpsExecutor.class);

    private ZpsTask task;
    private SharedData shared;
    private Process process = null;

    private Queue<ZpsScript> processQueue = Queues.newLinkedBlockingQueue();
    private int processCount = 1;
    private AtomicBoolean canceled = new AtomicBoolean(false);

    private List<ZpsReactionHandler> reactionHandlers = Lists.newArrayList();

    public MetaZpsExecutor(ZpsTask task, SharedData shared) {
        this.task = task;
        this.shared = shared;
    }

    public MetaZpsExecutor(ZpsTask task, String shared) {
        this.task = task;
        this.shared = new SharedData(shared);
    }

    public void addReactionHandler(ZpsReactionHandler handler) {
        reactionHandlers.add(handler);
    }

    public void removeReactionHandler(ZpsReactionHandler handler) {
        reactionHandlers.remove(handler);
    }

    public int execute() {

        logger.info("loading ZPS script : {}", task.getScriptPath());
        ZpsScript script;
        try {
            script = Json.Mapper.readValue(new File(task.getScriptPath()), ZpsScript.class);
        } catch (IOException e) {
            throw new ZpsException("Invalid ZPS script, " + e, e);
        }

        int exit = 1;
        Stopwatch timer = Stopwatch.createStarted();
        try {
            String scriptPath = task.getScriptPath();
            for (; ; ) {
                String lang = determineLanguagePlugin(script);

                logger.debug("running script with language: {}", lang);
                task.setCurrentScript(scriptPath);

                String[] command = createCommand(task, scriptPath, lang);
                exit = runProcess(command);

                if (exit != 0) {
                    break;
                }

                ZpsScript next = processQueue.poll();
                if (next == null) {
                    break;
                }

                script = next;
                processCount+=1;
                scriptPath = String.format("%s.%d", task.getScriptPath(),  processCount);

                logger.info("Writing next script {}", scriptPath);
                Files.deleteIfExists(Paths.get(scriptPath));
                Json.Mapper.writeValue(new File(scriptPath), script);
            }

        } catch (Exception e) {
            logger.warn("Failed to execute process: ", e);
            exit = 1;
        } finally {
            processQueue.clear();
            logger.info("Task stopped, exit status: {} in {}ms", exit,
                    timer.stop().elapsed(TimeUnit.MILLISECONDS));
        }

        return exit;
    }

    public String[] createCommand(ZpsTask task, String scriptPath, String lang) throws IOException {
        String absSharedPath =  FileUtils.normalize(shared.getRoot().toString());
        ImmutableList.Builder<String> b = ImmutableList.<String>builder()
                .add(String.format("%s/plugins/lang-%s/bin/zpsgo", absSharedPath, lang))
                .add("--shared-path", absSharedPath)
                .add("--script", scriptPath);

        if (task.getArgs() != null) {
            task.getArgs().forEach((k, v) -> {
                if (v != null) {
                    b.add("--global", k.concat("=").concat(v.toString()));
                }
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

    private static final String SEPERATOR = "##############################################################";

    public ProcessBuilder buildProcess(String[] command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Map<String, String> environment = builder.environment();
        environment.put("ZORROA_CLUSTER_PATH_SHARED", shared.getRoot().toString());
        environment.put("ZORROA_CLUSTER_PATH_CERTS", shared.resolvestr("certs"));
        environment.put("ZORROA_CLUSTER_PATH_OFS", shared.resolvestr("ofs"));
        environment.put("ZORROA_CLUSTER_PATH_PLUGINS", shared.resolvestr("plugins"));
        environment.put("ZORROA_CLUSTER_PATH_MODELS", shared.resolvestr("models"));

        List<String> pythonPaths = Lists.newArrayList();
        String pluginPath = shared.resolvestr("plugins");

        /**
         * List all plugins and then look for a site-packages directory
         * within them.  If one is found, add to PYTHONPATH evn var.
         */
        try (Stream<Path> files = Files.list(Paths.get(pluginPath))) {
            files.forEach(pluginRoot-> {
                Path sitePackages = pluginRoot.resolve("site-packages");
                if (Files.exists(sitePackages)) {
                    pythonPaths.add(sitePackages.toString());
                }
            });
        }

        if (!pythonPaths.isEmpty()) {
            environment.put("PYTHONPATH", String.join(":", pythonPaths));
        }

        if (task.getEnv() != null) {
            for(Map.Entry<String,String> entry: task.getEnv().entrySet()) {
                if (entry.getValue() == null) {
                    logger.warn("Failed to set null env var: {}", entry.getKey());
                    continue;
                }
                environment.put(entry.getKey(), entry.getValue());
            }
        }

        environment.put("PATH", appendToEnv(environment.get("PATH"),
                shared.resolvestr("bin")));

        return builder;
    }

    public int runProcess(String[] command) throws IOException {

        int exit = 1;
        Stopwatch timer = Stopwatch.createStarted();

        /*
         * Before running any process we check to see if we should still be running.
         */
        if (canceled.get()) {
            logger.warn("The process was stopped and is no longer valid.");
            return 13;
        }

        PrintWriter logStream = null;
        try {

            if (task.getLogPath() != null) {
                logStream = new PrintWriter(new FileOutputStream(new File(task.getLogPath()), processCount > 1));
            }
            else {
                logStream = new PrintWriter(System.out);
            }

            writeHeader(logStream, command);

            process = buildProcess(command).start();

            LineReader reader = new LineReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = null;
            boolean buffer = false;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(ZpsScript.SUFFIX)) {
                    try {
                        processBuffer(sb.toString());
                        buffer = false;
                        sb = null;
                    } catch (Exception e) {
                        /**
                         * The buffer cannot be parsed for some reason, we'll just fail the task.
                         */
                        logger.warn("Failed to process buffer {}, unexpected ", sb.toString(), e);
                        process.destroyForcibly();
                        break;
                    }
                } else if (buffer) {
                    sb.append(line);
                } else if (line.startsWith(ZpsScript.PREFIX)) {
                    buffer = true;
                    sb = new StringBuilder(1024);
                } else if (!line.isEmpty()) {
                    logStream.println(line);
                }
            }

        } catch (Exception e) {
            logger.warn("Unexpected error while running process {}",  task.getScriptPath(), e);

            writeProcessError(logStream, e);

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

            try {
                writeFooter(timer, logStream, exit);
            } catch (Exception e) {
                logger.warn("Error writing footer, ", e);
            }

            /*
             * Only close if there was a log file.
             */
            if (task.getLogPath()  != null) {
                Closeables.close(logStream, true);
            }
        }

        return exit;
    }


    public void writeHeader(PrintWriter logStream, String[] command) throws IOException {

        logStream.println(SEPERATOR);
        logStream.println("ZPS " + new Date().toString());

        StringBuilder sb = new StringBuilder(1024);
        sb.append("COMMAND: ");
        sb.append(String.join(" ", command));
        logStream.println(sb.toString());

        for (Map.Entry<String, String> e : task.getEnv().entrySet()) {
            // Hide this env var.
            if (e.getKey().equals("ZORROA_HMAC_KEY")) {
                continue;
            }
            logStream.println(new StringBuilder(256)
                    .append("ENV: ")
                    .append(e.getKey())
                    .append("=")
                    .append(e.getValue())
                    .toString());
        }

        logStream.println(SEPERATOR);
    }

    public void writeProcessError(PrintWriter logStream, Exception e) throws IOException {
        emitReaction(new Reaction()
                .setError(new ZpsError()
                        .setClassName("zpsgo")
                        .setMessage("Failed to start process: " + e.getMessage())
                        .setLineNumber(0)
                        .setPhase("zpsgo")
                        .setSkipped(true)));

        logStream.println("Failed to execute command:");
        e.printStackTrace(logStream);
    }

    public void writeFooter(Stopwatch timer, PrintWriter logStream, int exit) throws IOException {
        logStream.println(SEPERATOR);
        logStream.println(String.format("Duration: %.2f minutes", timer.elapsed(TimeUnit.SECONDS) / 60.0));
        logStream.println(String.format("Log: %s", task.getLogPath()));
        logStream.println(String.format("Script: %s", task.getScriptPath()));
        logStream.println(String.format("Exit Status: %d", exit));
        logStream.println(SEPERATOR);
    }

    public void processBuffer(String scriptText) throws IOException {
        /**
         * Parse the string into a Reaction.  If it doesn't parse, an exception is thrown
         * out to the I/O loop, which is handled there.
         */
        Reaction reaction = Json.deserialize(scriptText, Reaction.class);

        if (reaction.getNextProcess() != null) {
            processQueue.add(reaction.getNextProcess());
            logger.info("{} waiting in process queue", processQueue.size());
            return;
        }

        emitReaction(reaction);
    }

    public void emitReaction(Reaction reaction) {
        for (ZpsReactionHandler handler: reactionHandlers) {
            try {
                handler.handle(task, shared, reaction);
            }
            catch (Exception e) {
                logger.warn("{} failed to handle reaction, {}", e.getMessage(), e);
            }
        }
    }

    public boolean cancel() {
        boolean result = canceled.compareAndSet(false, true);
        if (result) {
            if (process != null) {
                /**
                 * Only manually kill pids on linux/mac
                 */
                if (!getPlatform().equals("windows")) {
                    List<Integer> pids = getProcessTree(task.getCurrentScript());
                    logger.info("Killing {} child {} PIDs {}", pids.size(), pids);
                    for (int pid : pids) {
                        try {
                            new ProcessBuilder(ImmutableList.of("kill", "-9", String.valueOf(pid)))
                                    .start().waitFor(5000, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException | IOException e) {
                            logger.warn("Failed to kill process: {}", e);
                        }
                    }
                }
                process.destroyForcibly();
            }
        }
        return result;
    }

    public static final String appendToEnv(String orig, String value) {
        if (orig == null || orig.isEmpty()) {
            return value;
        }
        else {
            return String.join(":", orig, value);
        }
    }

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static String getPlatform() {
        if (OS.contains("win")) {
            return "windows";
        }
        else if (OS.contains("nux")) {
            return "linux";
        }
        else if (OS.contains("mac")) {
            return "osx";
        }
        return "linux";
    }

    public List<Integer> getProcessTree(String scriptPath) {

        Multimap<Integer, Integer> map = ArrayListMultimap.create();
        List<Integer> result = Lists.newArrayList();
        Integer start = null;

        ProcessBuilder builder = new ProcessBuilder(
                ImmutableList.of("ps", "-a", "-o", "pid=", "-o", "ppid=", "-o", "command="));
        try {
            Process ps = builder.start();
            LineReader reader = new LineReader(new InputStreamReader(ps.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim().replaceAll("\\s{2,}", " ");
                String[] e = line.split("\\s", 3);

                int pid = Integer.valueOf(e[0]);
                int ppid = Integer.valueOf(e[1]);

                map.put(ppid, pid);
                if (e[2].contains(scriptPath)) {
                    start = pid;
                }
            }

            Queue<Integer> pending = new ArrayDeque<>();
            pending.add(start);
            while (!pending.isEmpty()) {
                Integer current = pending.poll();
                if (current == null) {
                    break;
                }
                Collection<Integer> children = map.get(current);
                if (children != null) {
                    result.addAll(children);
                    pending.addAll(children);
                }
            }
            Collections.reverse(result);
        } catch (Exception e) {
            logger.warn("Unable to find process tree for script, {}", scriptPath, e.getMessage());
        }
        return result;
    }
}
