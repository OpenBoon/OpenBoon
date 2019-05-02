package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by chambers on 2/16/16.
 */
public interface Loggable<T> {

    @JsonIgnore
    T getTargetId();

    @JsonIgnore
    default String getTargetType() {
        return this.getClass().getSimpleName().toLowerCase();
    }
}
