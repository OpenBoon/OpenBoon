package com.zorroa.common.domain;

/**
 * Created by chambers on 11/17/16.
 */
public class ExecuteTaskRequest {

    private String id;
    private String url;
    private int count;

    public String getId() {
        return id;
    }

    public ExecuteTaskRequest setId(String id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public ExecuteTaskRequest setUrl(String url) {
        this.url = url;
        return this;
    }

    public int getCount() {
        return count;
    }

    public ExecuteTaskRequest setCount(int count) {
        this.count = count;
        return this;
    }
}
