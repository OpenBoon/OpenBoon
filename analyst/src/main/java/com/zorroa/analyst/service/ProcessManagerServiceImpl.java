package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.zorroa.analyst.Application;
import com.zorroa.analyst.ArchivistClient;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.*;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.common.repository.EventLogDao;
import com.zorroa.sdk.processor.Reaction;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsExecutor;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


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
    EventLogDao eventLogDao;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    ListeningExecutorService analyzeExecutor;

    @Override
    public void queueExecute(ExecuteTaskStart task) {
        analyzeExecutor.execute(()->execute(task));
    }

    @Override
    public int execute(ExecuteTaskStart task) {
        ZpsScript script = Json.deserialize(task.getScript(), ZpsScript.class);
        task.putToEnv("ZORROA_ARCHIVIST_URL", properties.getString("analyst.master.host"));

        int exit = 1;
        try {
            if (!Application.isUnitTest()) {
                /**
                 * Don't run the actual command during a unit test
                 * since the language plugin is probably not installed.
                 */
                archivistClient.reportTaskStarted(new ExecuteTaskStarted(task));
                String lang = determineLanguagePlugin(script);
                logger.debug("running script with language: {}", lang);
                String[] command = createCommand(script, task, lang);
                logger.info("running command: {}", String.join(" ", command));
                exit = runProcess(command, task);
            }
            else {
                // unittest
                exit = 0;
            }
        } catch (Exception e) {
            // don't throw anything, just log
            logger.warn("Failed to execute process: ", e);
            exit=1;
        }
        finally {
            logger.info("Completed task: {} job:{}", task.getTaskId(), task.getJobId());
            if (logger.isDebugEnabled()) {
                logger.debug("Completed {}", Json.prettyString(script));
            }

            if (!Application.isUnitTest()) {
                archivistClient.reportTaskStopped(new ExecuteTaskStopped(task, exit));
            }
        }

        return exit;
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

    public int runProcess(String[] command, ExecuteTaskStart task) throws IOException {

        ProcessBuilder builder = new ProcessBuilder(command);
        if (task.getEnv() != null) {
            Map<String,String> env = builder.environment();
            for (Map.Entry<String, String> e: task.getEnv().entrySet()) {
                env.put(e.getKey(), e.getValue());
            }
        }

        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder sb = null;
        boolean buffer = false;
        String line;

        try (FileOutputStream logStream = new FileOutputStream(new File(task.getLogPath()))) {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(ZpsExecutor.SUFFIX)) {
                    processBuffer(sb, task, logStream);
                    logStream.write(NEWLINE);
                    buffer = false;
                    sb.setLength(0);
                } else if (buffer) {
                    sb.append(line);
                } else if (line.startsWith(ZpsExecutor.PREFIX)) {
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

    public void processBuffer(StringBuilder sb, ExecuteTaskStart task, FileOutputStream log) throws IOException {
        String scriptText = sb.toString();

        // Double check it can be serialized.
        Reaction reaction = Json.deserialize(scriptText, Reaction.class);

        if (reaction.getExpand() != null) {
            logger.info("Processing expand from job: {}", task.getJobId());
            ZpsScript script  = reaction.getExpand();

            log.write(ZpsExecutor.PREFIX.getBytes());
            log.write(NEWLINE);
            log.write(Json.prettyString(script).getBytes());
            log.write(NEWLINE);
            log.write(ZpsExecutor.SUFFIX.getBytes());

            ExecuteTaskExpand st = new ExecuteTaskExpand();
            st.setScript(Json.serializeToString(script));
            st.setParentTaskId(task.getTaskId());
            st.setJobId(task.getJobId());
            st.setName(script.getName());
            archivistClient.expand(st);
        }

        if (reaction.getResponse() != null) {
            logger.info("Processing response from job: {}", task.getJobId());
            archivistClient.respond(new ExecuteTaskResponse(task, reaction.getResponse()));
        }
    }
}
