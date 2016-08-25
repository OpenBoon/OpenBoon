package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Job;
import com.zorroa.common.domain.ExecuteTaskResponse;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobExecutorService {

    void queueSchedule();

    void schedule();

    void handleResponse(ExecuteTaskResponse response);

    Object waitOnResponse(Job job) throws InterruptedException;

}
