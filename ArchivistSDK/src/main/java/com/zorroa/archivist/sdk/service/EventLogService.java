package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.EventLogMessage;
import com.zorroa.archivist.sdk.domain.Id;

/**
 * Created by chambers on 12/29/15.
 */
public interface EventLogService {

    void log(EventLogMessage message);

    void log(Id object, String message, Object... args);

    void log(String message, Object... args);

    void log(Id object, String message, Throwable ex, Object... args);

    void log(Asset asset, String message, Object... args);

    void log(Asset object, String message, Throwable ex, Object... args);

}
