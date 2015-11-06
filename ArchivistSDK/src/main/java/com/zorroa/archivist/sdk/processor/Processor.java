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

    /**
     * Ths function is called once at the end of the entire ingest/export process.  Its NOT called
     * on a per-asset basis.  The intent is that subclasses can override this, but its not
     * required.
     */
    public void teardown() { }

    /**
     * This function is called once and only once before an ingest/export process begins.  Throwing
     * an exception from init() means the Processor stack could not be initialized and the operation
     * will not run.
     */
    public void init() throws Exception { }
}
