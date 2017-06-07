package com.zorroa.archivist.service;

import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.EventLogSearch;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.archivist.repository.EventLogDao;
import com.zorroa.archivist.security.SecureSingleThreadExecutor;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.cluster.thrift.TaskErrorT;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 *
 */
@Service
public class EventLogServiceImpl implements EventLogService {

    private static final Logger logger = LoggerFactory.getLogger(EventLogServiceImpl.class);

    @Autowired
    JobService jobService;

    @Autowired
    EventLogDao eventLogDao;

    private final Executor executor = SecureSingleThreadExecutor.singleThreadExecutor();

    @Override
    public void log(UserLogSpec spec) {
        if (spec.getUser() == null) {
            try {
                User user = SecurityUtils.getUser();
                spec.setUser(user);
            } catch (Exception e) {
                logger.warn("No Security Context {} ", spec.getMessage(), e);
            }
        }
        eventLogDao.create(spec);
    }

    @Override
    public void logAsync(UserLogSpec spec) {
        if (ArchivistConfiguration.unittest) {
            log(spec);
        }
        else {
            executor.execute(() -> log(spec));
        }
    }

    @Override
    public void log(Task task, List<TaskErrorT> errors) {
        eventLogDao.create(task, errors);
        jobService.incrementJobStats(task.getJobId(), 0, errors.size(), 0);
        jobService.incrementTaskStats(task.getTaskId(), 0, errors.size(), 0);
    }

    @Override
    public void logAsync(Task task, List<TaskErrorT> errors) {
        if (ArchivistConfiguration.unittest) {
            log(task,errors);
        }
        else {
            executor.execute(() -> log(task, errors));
        }
    }

    @Override
    public PagedList<Map<String,Object>> getAll(String type, EventLogSearch search, Pager page) {
        return eventLogDao.getAll(type, search, page);
    }
}
