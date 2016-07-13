package com.zorroa.archivist.service;

import com.zorroa.sdk.zps.ZpsReaction;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobExecutorService {

    void react(ZpsReaction script);

    void schedule();
}
