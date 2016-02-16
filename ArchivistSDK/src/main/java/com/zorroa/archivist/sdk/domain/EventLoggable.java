package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 2/16/16.
 */
public interface EventLoggable {

    Object getLogId();

    default String getLogType() {
        return this.getClass().getSimpleName();
    }

}
