package com.zorroa.analyst.service;

import com.zorroa.analyst.AnalystProcess;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.ExecuteTaskStop;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by chambers on 2/8/16.
 */
public interface ProcessManagerService {

    Collection<AnalystProcess> getProcesses();

    List<Integer> getTaskIds();

    boolean stopTask(ExecuteTaskStop task);

    void asyncStopTask(ExecuteTaskStop task);

    Future<AnalystProcess> execute(ExecuteTaskStart script, boolean async);

}
