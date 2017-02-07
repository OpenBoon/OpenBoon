package com.zorroa.analyst;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Queues;
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
    private TaskState newState = null;
    private Queue<ZpsScript> processQueue;
    private int processCount = 1;
    private Integer taskId;

    public AnalystProcess() {}

    public AnalystProcess(Integer taskId) {
        this.taskId = taskId;
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
        if (this.processQueue == null) {
            this.processQueue = Queues.newArrayDeque();
        }
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

    public Integer getTaskId() {
        return taskId;
    }

    public AnalystProcess setTaskId(Integer taskId) {
        this.taskId = taskId;
        return this;
    }
}
