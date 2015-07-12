package com.zorroa.archivist.domain;

import java.util.List;

/**
 * Created by chambers on 7/12/15.
 */
public class ProxyConfigUpdateBuilder {

    private String name;
    private String description;
    private List<ProxyOutput> outputs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ProxyOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ProxyOutput> outputs) {
        this.outputs = outputs;
    }
}
