package com.zorroa.archivist.domain;

/**
 * Created by chambers on 2/16/16.
 */
public interface Loggable<T> {

    T getTargetId();

    default String getTargetType() {
        return this.getClass().getSimpleName().toLowerCase();
    }
}
