package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.*;
import com.zorroa.common.domain.JobId;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.domain.TaskId;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobDao {

    int nextId();

    Job create(JobSpec spec);

    Job get(int id);

    Job get(JobId script);

    PagedList<Job> getAll(Paging page, JobFilter filter);

    long count(JobFilter filter);

    long count();

    boolean incrementStats(int id, int created, int updated, int errors, int warnings);

    void incrementWaitingTaskCount(JobId job);

    boolean setState(JobId job, JobState newState, JobState expect);

    JobState updateTaskStateCounts(TaskId task, TaskState value, TaskState expect);
}
