package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by chambers on 2/3/16.
 */
public abstract class SetSchema<E> extends ForwardingSet<E> {

    private final Set<E> delegate;

    public SetSchema() {
        delegate = Sets.newHashSet();
    }

    @Override
    @JsonIgnore
    protected Set<E> delegate() {
        return delegate;
    }
}
