package com.zorroa.analyst.service;

import com.zorroa.analyst.cluster.ClusterProcess;
import com.zorroa.common.cluster.thrift.TaskKillT;
import com.zorroa.common.cluster.thrift.TaskStartT;

import java.util.List;

/**
 * Created by chambers on 5/5/17.
 */
public interface ProcessManagerNgService {

    ClusterProcess queueClusterTask(TaskStartT task);
    ClusterProcess executeClusterTask(TaskStartT task);

    List<Integer> getTaskIds();

    void kill(TaskKillT kill);

    void killAllTasks();
}
