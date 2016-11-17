package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.Task;
import com.zorroa.common.domain.ExecuteTaskRequest;
import com.zorroa.common.domain.ExecuteTaskResponse;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.JobId;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobExecutorService {

    Future<List<ExecuteTaskStart>> queueWaitingTasks(ExecuteTaskRequest req);

    List<ExecuteTaskStart> getWaitingTasks(ExecuteTaskRequest req);

    void handleResponse(ExecuteTaskResponse response);

    Object waitOnResponse(Job job) throws InterruptedException;

    @Async
    void retryTask(Task task);

    @Async
    void skipTask(Task task);

    boolean cancelJob(JobId job);

    boolean restartJob(JobId job);
}
