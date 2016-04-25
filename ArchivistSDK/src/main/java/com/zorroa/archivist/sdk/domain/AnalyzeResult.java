package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * No need for getters/setters in this class
 */
public class AnalyzeResult {

    public int tried = 0;
    public int created = 0;
    public int updated = 0;
    public int warnings = 0;
    public int retries = 0;
    public int errors = 0;

    @JsonIgnore
    public List<String> logs = Lists.newArrayList();

    public AnalyzeResult add(AnalyzeResult other) {
        tried+=other.tried;
        created+=other.created;
        updated+=other.updated;
        warnings+=other.warnings;
        errors+=other.errors;
        retries+=other.retries;
        logs.addAll(other.logs);
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tried", tried)
                .add("created", created)
                .add("updated", updated)
                .add("warnings", warnings)
                .add("errors", errors)
                .add("retries", retries)
                .toString();
    }
}
