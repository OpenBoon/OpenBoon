package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.EventLogSearch;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.common.cluster.thrift.TaskErrorT;
import com.zorroa.common.domain.TaskId;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 5/19/17.
 */
public interface EventLogDao {
    PagedList<Map<String,Object>> getAll(String type, EventLogSearch search, Pager page);

    void create(UserLogSpec spec);

    void create(TaskId task, List<TaskErrorT> errors);
}
