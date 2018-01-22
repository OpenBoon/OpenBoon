package com.zorroa.cloudproxy.domain;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zorroa.cluster.zps.MetaZpsExecutor;
import com.zorroa.cluster.zps.ZpsReactionHandler;
import com.zorroa.cluster.zps.ZpsTask;
import com.zorroa.sdk.processor.Expand;
import com.zorroa.sdk.processor.Reaction;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by chambers on 3/27/17.
 */
public class ImportTask {

    private static final Logger logger = LoggerFactory.getLogger(ImportTask.class);

    private Path zpsScript;
    private Path sharedPath;
    private Path workDir;
    private Map<String, Object> args;
    private Map<String, String> env;
    private AtomicBoolean canceled = new AtomicBoolean(false);
    private Set<MetaZpsExecutor> executors = Collections.synchronizedSet(Sets.newHashSet());

    private ImportExecutor threadPool;

    public ImportTask(String scriptPath,
                      String sharedPath,
                      Settings props,
                      Map<String, Object> args) {
        this.zpsScript = Paths.get(scriptPath);
        this.sharedPath = Paths.get(sharedPath);
        this.workDir = this.sharedPath.resolve("jobs/" + UUID.randomUUID().toString());
        this.threadPool = new ImportExecutor(props.getThreads());

        logger.info("Starting task");
        for (Map.Entry<String, Object> e: args.entrySet()) {
            logger.info("ARG {}={}", e.getKey(), e.getValue());
        }

        this.args = args;

        this.env = Maps.newHashMap();
        this.env.put("ZORROA_ARCHIVIST_URL", props.getArchivistUrl());
        this.env.put("ZORROA_HMAC_KEY", props.getHmacKey());
        this.env.put("ZORROA_USER", props.getAuthUser());
        this.env.put("ZORROA_WORK_DIR", workDir.toString());
    }

    public void start() {
        MetaZpsExecutor zpsExecutor = null;
        try {
            FileUtils.makedirs(workDir);
            ZpsTask zpsTask = new ZpsTask()
                    .setArgs(args)
                    .setEnv(env)
                    .setScriptPath(zpsScript.toString());

            zpsExecutor = new MetaZpsExecutor(zpsTask, new SharedData(sharedPath));
            zpsExecutor.addReactionHandler(new ReactionHandler());
            executors.add(zpsExecutor);
            logger.info("Executing ZPS script");
            zpsExecutor.execute();

            logger.info("Waiting for import task process to complete.");
            threadPool.waitForCompletion();
        }
        catch (Exception e) {
            logger.warn("Failed to execute:", e);
        }
        finally {
            if (zpsExecutor != null) {
                executors.remove(zpsExecutor);
            }
        }
    }

    public void cancel() {
        if (canceled.compareAndSet(false, true)) {
            logger.info("Canceling running ZPS script");

            for (MetaZpsExecutor mze: executors) {
                mze.cancel();
            }
            logger.info("Forcing shutdown of ImportTask threads for job #{}", args.get("jobId"));
            threadPool.shutdownNow();
        }
    }

    public Map<String,Object> getProgress() {
        return ImmutableMap.of(
                "total", threadPool.getTaskCount(),
                "completed", threadPool.getCompletedTaskCount(),
                "progress", threadPool.getCompletedTaskCount() / (double) threadPool.getTaskCount());
    }

    private void expand(Expand expand) {
        if (canceled.get()) {
            return;
        }

        for (;;) {
            try {
                threadPool.execute(() -> {
                    Path scriptPath = null;
                    MetaZpsExecutor zpsExecutor = null;
                    try {
                        if (expand.getFrames() != null) {
                            logger.info("Processing {} assets", expand.getFrames().size());
                        }
                        scriptPath = workDir.resolve(UUID.randomUUID().toString() + ".zps");
                        Json.Mapper.writeValue(scriptPath.toFile(),
                                new ZpsScript()
                                        .setOver(expand.getDocuments())
                                        .setExecute(expand.getExecute())
                                        .setName(expand.getName()));

                        ZpsTask zpsTask = new ZpsTask()
                                .setArgs(args)
                                .setEnv(env)
                                .setScriptPath(scriptPath.toString());
                        zpsExecutor = new MetaZpsExecutor(zpsTask, new SharedData(sharedPath));
                        executors.add(zpsExecutor);
                        zpsExecutor.execute();
                    }
                    catch (Exception e) {
                        logger.warn("Failed to process assets, unexepected", e);
                    }
                    finally {
                        if (zpsExecutor != null) {
                            executors.remove(zpsExecutor);
                        }
                        if (scriptPath != null) {
                            try {
                                Files.delete(scriptPath);
                            } catch (IOException e) {
                                logger.warn("Failed to delete script path", e);
                            }
                        }
                    }
                });
                logger.info("Submitted expand");
                return;
            }
            catch (RejectedExecutionException e) {
                try {
                    logger.warn("Execution queue full, waiting 5 seconds...");
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    return;
                }
            }
        }
    }

    public Path getWorkDir() {
        return workDir;
    }

    public ImportTask setWorkDir(Path workDir) {
        this.workDir = workDir;
        return this;
    }

    private class ReactionHandler implements ZpsReactionHandler {
        @Override
        public void handle(ZpsTask zpsTask, SharedData sharedData, Reaction reaction) {
            if (reaction.getExpand() != null) {
                expand(reaction.getExpand());
            }
        }
    }
}
