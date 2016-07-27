package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.common.repository.EventLogDao;
import com.zorroa.sdk.client.archivist.ArchivistClient;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsExecutor;
import com.zorroa.sdk.zps.ZpsReaction;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    public void queueExecute(ZpsScript script) {
        analyzeExecutor.execute(()->execute(script));
    }

    @Override
    public void execute(ZpsScript script) {
        execute(script, ImmutableMap.of());
    }

    @Override
    public void execute(ZpsScript script, Map<String,Object> args) {

        archivistClient.reportTaskRunning(script);

        int exit = 1;
        try {

            if (script.getArgs() == null) {
                script.setArgs(args);
            }
            else {
                script.getArgs().putAll(args);
            }

            String lang = determineLanguagePlugin(script);
            logger.debug("running script with language: {}", lang);
            String[] command = createCommand(script, lang);
            logger.info("running command: {}", String.join(" ", command));
            exit = runProcess(command);

        } catch (Exception e) {
            // don't throw anything, just log
            logger.warn("Failed to execute process: ", e);
            exit=1;
        }
        finally {
            logger.info("Completed task: {} job:{}", script.getTaskId(), script.getJobId());
            if (logger.isDebugEnabled()) {
                logger.debug("Completed {}", Json.prettyString(script));
            }
            archivistClient.reportTaskCompleted(script, exit);
        }
    }

    public String[] createCommand(ZpsScript script, String lang) throws IOException {
        ImmutableList.Builder<String> b = ImmutableList.<String>builder()
                .add(String.format("%s/lang-%s/bin/zpsgo", properties.getString("zorroa.cluster.path.plugins"), lang))
                .add("-shared-path", properties.getString("zorroa.cluster.path.shared"))
                .add("-plugin-path", properties.getString("zorroa.cluster.path.plugins"))
                .add("-model-path", properties.getString("zorroa.cluster.path.models"))
                .add("-storage-path", properties.getString("zorroa.cluster.path.storage"))
                .add("-export-path", properties.getString("zorroa.cluster.path.exports"))
                .add("-script", writeScript(script).toString());

        if (script.getArgs() != null) {
            script.getArgs().forEach((k, v) -> {
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
        /**
         * TODO: support generators/reducers with multiple languages.
         */

        switch (script.getExecute()) {
            case "generate":
                return script.getGenerate().get(0).getLanguage();
            case "pipeline":
                if (script.getPipeline() == null || script.getPipeline().isEmpty()) {
                    return "java";
                }
                else {
                    return script.getPipeline().get(0).getLanguage();
                }
            default:
                throw new RuntimeException("Invalid script execution: " + script.getExecute());
        }
    }

    public int runProcess(String[] command) throws IOException {

        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder sb = null;
        boolean buffer = false;
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(ZpsExecutor.SUFFIX)) {
                processBuffer(sb);
                buffer = false;
                sb.setLength(0);
            }
            else if (buffer) {
                sb.append(line);

            }
            else if (line.startsWith(ZpsExecutor.PREFIX)) {
                buffer = true;
                sb = new StringBuilder(8096);
            }
        }

        int exit = 0;
        try {
            exit = process.waitFor();
        } catch (InterruptedException e) {
            logger.warn("Process interrupted: ", e);
            exit = 1;
        }
        return exit;
    }

    public void processBuffer(StringBuilder sb) {
        ZpsReaction reaction = Json.deserialize(sb.toString(), ZpsReaction.class);
        if (logger.isDebugEnabled()) {
            logger.debug("Reaction: {}", Json.prettyString(reaction));
        }
        archivistClient.react(reaction);
    }
}
