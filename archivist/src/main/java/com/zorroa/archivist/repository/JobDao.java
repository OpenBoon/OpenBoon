package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.*;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.zps.ZpsJob;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobDao {

    ZpsScript create(ZpsScript job, PipelineType type);

    Job get(int id);

    Job get(ZpsScript script);

    PagedList<Job> getAll(Paging page, JobFilter filter);

    long count(JobFilter filter);

    long count();

    boolean incrementStats(int id, int created, int updated, int errors, int warnings);

    void incrementWaitingTaskCount(ZpsTask task);

    boolean setState(ZpsJob job, JobState newState, JobState expect);

    JobState updateTaskStateCounts(ZpsTask task, TaskState value, TaskState expect);
}
