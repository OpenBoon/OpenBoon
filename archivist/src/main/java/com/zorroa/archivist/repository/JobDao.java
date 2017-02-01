package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobFilter;
import com.zorroa.archivist.domain.JobSpec;
import com.zorroa.archivist.domain.JobState;
import com.zorroa.common.domain.JobId;
import com.zorroa.common.domain.TaskId;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobDao {

    JobSpec nextId(JobSpec spec);

    Job create(JobSpec spec);

    Job get(int id);

    Job get(JobId script);

    PagedList<Job> getAll(Pager page, JobFilter filter);

    long count(JobFilter filter);

    long count();

    boolean incrementStats(int id, int success, int errors, int warnings);

    void incrementWaitingTaskCount(JobId job);

    boolean setState(JobId job, JobState newState, JobState expect);

    JobState getState(int id);

    String getRootPath(int id);

    void updateTaskStateCounts(TaskId task, TaskState value, TaskState expect);
}
