package com.zorroa.archivist.sdk.processor;

import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;

import java.util.Map;

/**
 * Created by chambers on 11/2/15.
 */
public class Processor {

    protected Map<String, Object> args;
    protected ApplicationProperties applicationProperties;

    public Processor() {
        this.args = Maps.newHashMap();
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public <T> T getArg(String key) {
        return (T) this.args.get(key);
    }

    public void setArg(String key, Object value) {
        this.args.put(key, value);
    }

    public ApplicationProperties getApplicationProperties() {
        return applicationProperties;
    }

    public Processor setApplicationProperties(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        return this;
    }

    /**
     * Ths function is called once at the end of the entire ingest/export process.  Its NOT called
     * on a per-asset basis.  The intent is that subclasses can override this, but its not
     * required.
     */
    public void teardown() { }
}
