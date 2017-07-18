package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.EventLogSearch;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.common.cluster.thrift.TaskErrorT;
import com.zorroa.sdk.domain.PagedList;

import java.util.List;
import java.util.Map;

public interface EventLogService {

    void log(UserLogSpec spec);

    void logAsync(UserLogSpec spec);

    void log(Task task, List<TaskErrorT> errors);

    void logAsync(Task task, List<TaskErrorT> errors);

    PagedList<Map<String,Object>> getAll(String type, EventLogSearch search);

}
