package com.zorroa.archivist.domain;

import java.util.List;

public class ProxyConfig {

    private String id;
    private long version;
    private List<ProxyOutput> outputs;

    public List<ProxyOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ProxyOutput> outputs) {
        this.outputs = outputs;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
