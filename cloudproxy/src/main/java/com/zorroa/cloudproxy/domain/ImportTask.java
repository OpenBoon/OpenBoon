package com.zorroa.cloudproxy.domain;

import com.google.common.collect.Maps;
import com.zorroa.sdk.processor.Reaction;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.MetaZpsExecutor;
import com.zorroa.sdk.zps.ZpsReactionHandler;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by chambers on 3/27/17.
 */
public class ImportTask {

    private static final Logger logger = LoggerFactory.getLogger(ImportTask.class);

    private Path zpsScript;
    private Path sharedPath;
    private Path workDir;
    private Settings props;
    private Map<String, Object> args;
    private Map<String, String> env;

    private MetaZpsExecutor zpsExecutor;
    private AssetExecutor threadPool;

    public ImportTask(String scriptPath,
                      String sharedPath, Settings configProps, ImportStatus status) {
        this.zpsScript = Paths.get(scriptPath);
        this.sharedPath = Paths.get(sharedPath);
        this.props = configProps;
        this.workDir = this.sharedPath.resolve("jobs/" + UUID.randomUUID().toString());
        this.threadPool = new AssetExecutor(props.getThreads());
        args = Maps.newHashMap();
        args.put("path", props.getPaths().get(0));
        args.put("cutOffTime", status.getStartTime());
        args.put("pipelineId", props.getPipelineId());
        args.put("jobId", status.getCurrentJobId());

        logger.info("starting task");
        logger.info("Path: {}", props.getPaths().get(0));
        logger.info("CutOFf: {}", status.getStartTime());
        logger.info("pipelineId: {}", props.getPipelineId());
        logger.info("jobId: {}", status.getCurrentJobId());

        env = Maps.newHashMap();
        env.put("ZORROA_ARCHIVIST_URL", props.getArchivistUrl());
        env.put("ZORROA_HMAC_KEY", props.getHmacKey());
        env.put("ZORROA_USER", props.getAuthUser());
        env.put("ZORROA_WORK_DIR", workDir.toString());
    }

    public void start() {
        try {
            FileUtils.makedirs(workDir);
            ZpsTask zpsTask = new ZpsTask()
                    .setArgs(args)
                    .setEnv(env)
                    .setScriptPath(zpsScript.toString());

            zpsExecutor = new MetaZpsExecutor(zpsTask, new SharedData(sharedPath));
            zpsExecutor.addReactionHandler(new ReactionHandler());
            logger.info("Executing ZPS script");
            zpsExecutor.execute();

            logger.info("Waiting for import task process to complete.");
            threadPool.waitForCompletion();
        }
        catch (Exception e) {
            logger.warn("Failed to execute:", e);
        }
    }

    private void expand(ZpsScript expand) {
        for (;;) {
            try {
                threadPool.execute(() -> {
                    Path scriptPath = null;
                    try {
                        if (expand.getOver() != null) {
                            logger.info("Processing {} assets", expand.getOver().size());
                        }
                        scriptPath = workDir.resolve(UUID.randomUUID().toString() + ".zps");
                        Json.Mapper.writeValue(scriptPath.toFile(), expand);

                        ZpsTask zpsTask = new ZpsTask()
                                .setArgs(args)
                                .setEnv(env)
                                .setScriptPath(scriptPath.toString());
                        zpsExecutor = new MetaZpsExecutor(zpsTask, new SharedData(sharedPath));
                        zpsExecutor.execute();
                    }
                    catch (Exception e) {
                        logger.warn("Failed to process assets, unexepected", e);
                    }
                    finally {
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
