package com.zorroa.archivist.service;

import com.zorroa.archivist.SecureSingleThreadExecutor;
import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.repository.LogDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 *
 */
@Service
public class LogServiceImpl implements LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);

    @Autowired
    LogDao logDao;

    private final Executor executor = SecureSingleThreadExecutor.singleThreadExecutor();

    @Override
    public void log(LogSpec spec) {
        if (spec.getUser() == null) {
            try {
                User user = SecurityUtils.getUser();
                spec.setUser(user);
            } catch (Exception e) {
                logger.warn("No Security Context {} ", spec.getMessage(), e);
            }
        }
        logDao.create(spec);
    }

    @Override
    public void logAsync(LogSpec spec) {
        executor.execute(() -> log(spec));
    }

    @Override
    public PagedList<Map<String,Object>> search(LogSearch search, Pager page) {
        PagedList<Map<String,Object>> result =  logDao.search(search, page);
        return result;
    }

}
