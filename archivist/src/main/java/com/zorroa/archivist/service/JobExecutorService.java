package com.zorroa.archivist.service;

import com.zorroa.sdk.zps.ZpsScript;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobExecutorService {

    void expand(ZpsScript script);

    void schedule();
}
