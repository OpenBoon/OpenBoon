package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.Task;
import com.zorroa.common.cluster.thrift.TaskResultT;
import com.zorroa.common.cluster.thrift.TaskStartT;
import com.zorroa.archivist.domain.JobId;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobExecutorService {

    Future<List<TaskStartT>> queueWaitingTasks(String url, int count);

    List<TaskStartT> getWaitingTasks(String url, int count);

    void handleResponse(Task task, TaskResultT response);

    Object waitOnResponse(Job job) throws InterruptedException;

    @Async
    void retryTask(Task task);

    @Async
    void skipTask(Task task);

    boolean cancelJob(JobId job);

    boolean restartJob(JobId job);
}
