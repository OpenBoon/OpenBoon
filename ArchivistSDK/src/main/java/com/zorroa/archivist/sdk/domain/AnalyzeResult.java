package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;

/**
 * No need for getters/setters in this class
 */
public class AnalyzeResult {

    public int created = 0;
    public int updated = 0;
    public int warnings = 0;
    public int retries = 0;
    public int errors = 0;

    public AnalyzeResult add(AnalyzeResult other) {
        created+=other.created;
        updated+=other.updated;
        warnings+=other.warnings;
        errors+=other.errors;
        retries+=other.retries;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("created", created)
                .add("updated", updated)
                .add("warnings", warnings)
                .add("errors", errors)
                .add("retries", retries)
                .toString();
    }
}
