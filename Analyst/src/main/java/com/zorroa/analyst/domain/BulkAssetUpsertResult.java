package com.zorroa.analyst.domain;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * No need for getters/setters in this class
 */
public class BulkAssetUpsertResult {

    public int created = 0;
    public int updated = 0;
    public int errorsRecoverable = 0;
    public int errorsNotRecoverable = 0;
    public int retries = 0;
    public List<String> errors = Lists.newArrayList();

    public void add(BulkAssetUpsertResult other) {
        created+=other.created;
        updated+=other.updated;
        errorsRecoverable+=other.errorsRecoverable;
        errorsNotRecoverable+=other.errorsNotRecoverable;
        retries+=other.retries;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("created", created)
                .add("updated", updated)
                .add("errorsRecoverable", errorsRecoverable)
                .add("errorsNotRecoverable", errorsNotRecoverable)
                .add("retries", retries)
                .toString();
    }
}
