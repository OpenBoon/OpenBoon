package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.repository.LogDao;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 *
 */
@Service
public class LogServiceImpl implements LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);

    @Autowired
    LogDao logDao;

    private Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void log(LogSpec spec) {
        executor.execute(() -> logDao.create(spec));
    }

    @Override
    public PagedList<Map<String,Object>> search(LogSearch search, Pager page) {
        PagedList<Map<String,Object>> result =  logDao.search(search, page);
        return result;
    }

}
