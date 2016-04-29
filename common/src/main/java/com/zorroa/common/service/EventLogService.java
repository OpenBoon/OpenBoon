package com.zorroa.common.service;

import com.zorroa.sdk.domain.EventLogMessage;
import com.zorroa.sdk.domain.EventLoggable;

/**
 * Created by chambers on 12/29/15.
 */
public interface EventLogService {

    void setSynchronous(boolean synchronous);

    void log(EventLogMessage message);

    void log(EventLoggable object, String message, Object... args);

    void log(EventLoggable object, String message, Throwable ex, Object... args);

    void log(String message, Object... args);

    void log(String message, Throwable ex, Object... args);


}
