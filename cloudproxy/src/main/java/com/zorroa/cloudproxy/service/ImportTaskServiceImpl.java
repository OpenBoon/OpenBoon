package com.zorroa.cloudproxy.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.zorroa.cloudproxy.domain.ImportStatus;
import com.zorroa.cloudproxy.domain.ImportTask;
import com.zorroa.cloudproxy.domain.Settings;
import com.zorroa.sdk.client.ArchivistClient;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by chambers on 4/19/17.
 */
@Service
public class ImportTaskServiceImpl implements ImportTaskService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    @Value("${cloudproxy.paths.config}")
    private String configPath;

    @Value("${cloudproxy.paths.shared}")
    private String sharedPath;

    @Autowired
    SettingsService configService;

    AtomicReference<ImportTask> runningImportTask = new AtomicReference<>();

    ExecutorService importTaskExecutor = Executors.newSingleThreadExecutor();

    @Override
    public Future submitImportTask(boolean cleanup) {
        return importTaskExecutor.submit(()-> runImportTask(cleanup));
    }

    @Override
    public ImportTask runImportTask(boolean cleanup) {

        Settings configProps = configService.getSettings();
        if (configProps == null) {
            return null;
        }

        String scriptFile = configPath + "/script.zps";
        Integer jobId = launchJobAndReturnId(configProps);

        Map<String, Object> args = Maps.newHashMap();
        args.put("path", FileUtils.normalize(configProps.getPaths().get(0)));
        args.put("pipelineId", configProps.getPipelineId());
        args.put("jobId", jobId);

        ImportTask task = new ImportTask(scriptFile, sharedPath, configProps, args);
        setRunningImportTask(task, jobId);
        try {
            task.start();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start cloud proxy process, ", e);
        }
        finally {
            setRunningImportTask(null, null);
            logger.info("Task is complete");
            try {
                logger.info("Deleting work dir: {}", task.getWorkDir());
                FileUtils.deleteRecursive(task.getWorkDir().toFile());
            }
            catch (Exception e) {
                logger.warn("Failed to clean up last run data ", e);
            }
        }
        return task;
    }

    public Integer launchJobAndReturnId(Settings configProps) {
        /**
         * Set some system props that the archivist client can grab.
         */
        System.setProperty("zorroa.hmac.key", configProps.getHmacKey());
        System.setProperty("zorroa.user", configProps.getAuthUser());
        System.setProperty("zorroa.archivist.url", configProps.getArchivistUrl());

        ArchivistClient client = new ArchivistClient();
        Map<String, Object> job = client.getConnection().post("/api/v1/jobs",
                ImmutableMap.of(
                        "name", "CloudProxyImport",
                        "type", "Import",
                        "script", new ZpsScript()), Map.class);
        logger.info("launched job on remote archivist, ID# {}", job.get("jobId"));
        return (Integer) job.get("jobId");
    }

    @Override
    public void cancelAllTasks() {
        cancelRunningImportTask();
        importTaskExecutor.shutdown();
        try {
            importTaskExecutor.awaitTermination(365, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            // ignore, system probably shutting down.
        }
        importTaskExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean cancelRunningImportTask() {
        ImportTask task = runningImportTask.get();
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

    @Override
    public ImportTask getActiveImportTask() {
        return runningImportTask.get();
    }

    @Override
    public boolean isImportTaskRunning() {
        return runningImportTask.get() != null;
    }

    private synchronized void setRunningImportTask(ImportTask task, Integer jobId) {
        ImportStatus status = configService.getImportStats();
        if (task != null) {
            status.setStartTime(System.currentTimeMillis());
            status.setFinishTime(null);
            status.setCurrentJobId(jobId);
            status.setActive(true);
            runningImportTask.set(task);
        }
        else {
            status.setFinishTime(System.currentTimeMillis());
            status.setActive(false);
            runningImportTask.set(null);
        }

        configService.saveImportStats(status);
    }
}
