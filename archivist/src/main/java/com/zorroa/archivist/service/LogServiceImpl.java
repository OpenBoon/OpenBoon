package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.repository.LogDao;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.PagedElasticList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class LogServiceImpl implements LogService {

    @Autowired
    LogDao logDao;

    @Async
    @Override
    public void log(LogSpec spec) {
        logDao.create(spec);
    }


    @Override
    public PagedElasticList search(LogSearch search, Paging page) {
        return logDao.search(search, page);
    }

}
