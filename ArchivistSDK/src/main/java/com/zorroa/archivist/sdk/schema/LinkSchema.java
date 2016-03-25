package com.zorroa.archivist.sdk.schema;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by chambers on 3/25/16.
 */
public class LinkSchema {

    private Set<String> inputs;
    private Set<String> outputs;

    public LinkSchema() { }

    public LinkSchema(boolean allocate) {
        if (allocate) {
            inputs = Sets.newHashSet();
            outputs = Sets.newHashSet();
        }
    }

    public Set<String> getInputs() {
        return inputs;
    }

    public LinkSchema setInputs(Set<String> inputs) {
        this.inputs = inputs;
        return this;
    }

    public Set<String> getOutputs() {
        return outputs;
    }

    public LinkSchema setOutputs(Set<String> outputs) {
        this.outputs = outputs;
        return this;
    }
}
