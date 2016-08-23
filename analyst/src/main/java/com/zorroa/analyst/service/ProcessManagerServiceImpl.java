package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.zorroa.analyst.ArchivistClient;
import com.zorroa.common.domain.ExecuteTaskExpand;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.ExecuteTaskStarted;
import com.zorroa.common.domain.ExecuteTaskStopped;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.common.repository.EventLogDao;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsExecutor;
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
    public void queueExecute(ExecuteTaskStart task) {
        analyzeExecutor.execute(()->execute(task));
    }

    @Override
    public void execute(ExecuteTaskStart task) {
        ZpsScript script = Json.deserialize(task.getScript(), ZpsScript.class);
        task.putToEnv("ZORROA_ARCHIVIST_URL", properties.getString("analyst.master.host"));

        archivistClient.reportTaskStarted(new ExecuteTaskStarted(task));
        int exit = 1;
        try {
            String lang = determineLanguagePlugin(script);
            logger.debug("running script with language: {}", lang);
            String[] command = createCommand(script, task, lang);
            logger.info("running command: {}", String.join(" ", command));
            exit = runProcess(command, task, script);

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

            archivistClient.reportTaskStopped(new ExecuteTaskStopped(task, exit));
        }
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

    public int runProcess(String[] command, ExecuteTaskStart task, ZpsScript script) throws IOException {

        ProcessBuilder builder = new ProcessBuilder(command);
        /**
         * Dump STDERR or the process to this log.
         */
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

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

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(ZpsExecutor.SUFFIX)) {
                processBuffer(sb, task);
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

    public void processBuffer(StringBuilder sb, ExecuteTaskStart task) {
        String scriptText = sb.toString();
        // Double check it can be serialized.
        ZpsScript script = Json.deserialize(scriptText, ZpsScript.class);
        if (logger.isDebugEnabled()) {
            logger.debug("Reaction: {}", Json.prettyString(script));
        }
        ExecuteTaskExpand st = new ExecuteTaskExpand();
        st.setScript(sb.toString());
        st.setParentTaskId(task.getTaskId());
        st.setJobId(task.getJobId());
        st.setScript(scriptText);
        st.setName(script.getName());
        archivistClient.expand(st);
    }
}
