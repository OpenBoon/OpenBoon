package com.zorroa.analyst;

import com.zorroa.common.domain.TaskState;

import java.nio.file.Path;

/**
 * Created by chambers on 9/20/16.
 */
public class AnalystProcess {

    private Process process = null;
    private Path logFile = null;
    private TaskState newState = null;

    public AnalystProcess() {}

    public Process getProcess() {
        return process;
    }

    public AnalystProcess setProcess(Process process) {
        this.process = process;
        return this;
    }

    public Path getLogFile() {
        return logFile;
    }

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
}
