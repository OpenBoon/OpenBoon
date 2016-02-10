package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.domain.AnalystPing;

/**
 * Created by chambers on 2/9/16.
 */
public interface AnalystService {
    void register(AnalystPing ping);

    void shutdown(AnalystPing ping);
}
