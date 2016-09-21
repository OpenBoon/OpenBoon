package com.zorroa.common.domain;

/**
 * Created by chambers on 7/11/16.
 */
public enum TaskState {
    Waiting,
    Queued,
    Running,
    Success,
    Failure,
    Skipped;

    public static final boolean requiresStop(TaskState state) {
        return state.equals(TaskState.Queued) || state.equals(TaskState.Running);
    }
}
