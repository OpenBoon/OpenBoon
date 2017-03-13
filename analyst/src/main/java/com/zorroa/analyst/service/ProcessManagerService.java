package com.zorroa.analyst.service;

import com.zorroa.analyst.AnalystProcess;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.ExecuteTaskStop;
import com.zorroa.common.domain.TaskState;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by chambers on 2/8/16.
 */
public interface ProcessManagerService {

    Collection<AnalystProcess> getProcesses();

    List<Integer> getTaskIds();

    boolean stopTask(AnalystProcess p, TaskState newstate, String reason);

    void asyncStopTask(ExecuteTaskStop task);

    void stopAllTasks();

    Future<AnalystProcess> execute(ExecuteTaskStart script, boolean async);

}
