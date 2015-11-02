package com.zorroa.archivist.sdk.processor;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Created by chambers on 11/2/15.
 */
public class Processor {

    protected Map<String, Object> args;

    public Processor() {
        this.args = Maps.newHashMap();
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }
}
