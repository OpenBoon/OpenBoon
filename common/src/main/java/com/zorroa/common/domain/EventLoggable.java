package com.zorroa.common.domain;

/**
 * Created by chambers on 2/16/16.
 */
public interface EventLoggable {

    String getEventLogId();

    default String getEventLogType() {
        return this.getClass().getSimpleName();
    }

}
