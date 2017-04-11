package com.zorroa.analyst;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Queues;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.zps.ZpsScript;

import java.nio.file.Path;
import java.util.Queue;

/**
 * Created by chambers on 9/20/16.
 */
public class AnalystProcess {

    private Process process = null;
    private Path logFile = null;
    private volatile TaskState newState = null;
    private Queue<ZpsScript> processQueue = Queues.newLinkedBlockingQueue();
    private int processCount = 1;
    private ExecuteTaskStart task;
    private ArchivistClient client;

    public AnalystProcess(ExecuteTaskStart task) {
        this.task = task;
        this.client = new ArchivistClient(task.getArchivistHost());
    }

    @JsonIgnore
    public Process getProcess() {
        return process;
    }

    @JsonIgnore
    public AnalystProcess setProcess(Process process) {
        this.process = process;
        return this;
    }

    @JsonIgnore
    public Path getLogFile() {
        return logFile;
    }

    @JsonIgnore
    public AnalystProcess setLogFile(Path logFile) {
        this.logFile = logFile;
        return this;
    }

    public TaskState getNewState() {
        return newState;
    }

    public AnalystProcess setNewState(TaskState newState) {
        this.newState = newState;
        return this;
    }

    public AnalystProcess addToNextProcess(ZpsScript next) {
        this.processQueue.add(next);
        return this;
    }

    public ZpsScript nextProcess() {
        if (this.processQueue == null) {
            return null;
        }
        return processQueue.poll();
    }

    public int getProcessCount() {
        return processCount;
    }

    public AnalystProcess incrementProcessCount() {
        processCount++;
        return this;
    }

    public ExecuteTaskStart getTask() {
        return task;
    }

    public AnalystProcess setTask(ExecuteTaskStart task) {
        this.task = task;
        return this;
    }

    public ArchivistClient getClient() {
        return client;
    }

    public AnalystProcess setClient(ArchivistClient client) {
        this.client = client;
        return this;
    }
}


