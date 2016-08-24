package com.zorroa.analyst.service;

import com.zorroa.common.domain.ExecuteTaskStart;

/**
 * Created by chambers on 2/8/16.
 */
public interface ProcessManagerService {

    int execute(ExecuteTaskStart script);

    void queueExecute(ExecuteTaskStart script);

}
