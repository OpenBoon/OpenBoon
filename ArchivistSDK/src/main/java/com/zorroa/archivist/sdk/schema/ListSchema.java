package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by chambers on 12/11/15.
 */
public abstract class ListSchema<E> extends ForwardingList<E> implements Schema {

    final List<E> delegate;

    public ListSchema() {
        delegate = Lists.newArrayList();
    }

    @Override
    @JsonIgnore
    protected List<E> delegate() {
        return delegate;
    }

}
