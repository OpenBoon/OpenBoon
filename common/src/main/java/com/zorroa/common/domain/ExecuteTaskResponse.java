package com.zorroa.common.domain;

/**
 * Created by chambers on 8/25/16.
 */
public class ExecuteTaskResponse extends ExecuteTask {

    private Object response;

    public ExecuteTaskResponse() { }

    public ExecuteTaskResponse(ExecuteTask task, Object response) {
        super(task);
        this.response = response;
    }

    public Object getResponse() {
        return response;
    }

    public ExecuteTaskResponse setResponse(Object response) {
        this.response = response;
        return this;
    }
}
