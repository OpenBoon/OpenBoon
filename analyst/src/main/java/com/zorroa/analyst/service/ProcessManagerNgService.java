package com.zorroa.analyst.service;

import com.zorroa.analyst.cluster.ClusterProcess;
import com.zorroa.common.cluster.thrift.TaskKillT;
import com.zorroa.common.cluster.thrift.TaskResultT;
import com.zorroa.common.cluster.thrift.TaskStartT;

import java.io.IOException;
import java.util.List;

/**
 * Created by chambers on 5/5/17.
 */
public interface ProcessManagerNgService {

    ClusterProcess queueClusterTask(TaskStartT task);
    TaskResultT executeClusterTask(TaskStartT task) throws IOException;

    List<Integer> getTaskIds();

    void kill(TaskKillT kill);

    void killAllTasks();
}
