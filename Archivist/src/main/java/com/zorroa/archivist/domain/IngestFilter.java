package com.zorroa.archivist.domain;


import java.util.EnumSet;
import java.util.List;

public class IngestFilter {

    private EnumSet<IngestState> states;
    private int limit = -1;
    private List<String> pipelines;

    public EnumSet<IngestState> getStates() {
        return states;
    }

    public IngestFilter setStates(EnumSet<IngestState> states) {
        this.states = states;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public IngestFilter setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<String> getPipelines() {
        return pipelines;
    }

    public IngestFilter setPipelines(List<String> pipelines) {
        this.pipelines = pipelines;
        return this;
    }
}
