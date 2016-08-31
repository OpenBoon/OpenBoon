package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.repository.LogDao;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.ElasticPagedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 *
 */
@Service
public class LogServiceImpl implements LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);

    @Autowired
    LogDao logDao;

    @Async
    @Override
    public void log(LogSpec spec) {
        logDao.create(spec);
    }


    @Override
    public ElasticPagedList<Map<String,Object>> search(LogSearch search, Paging page) {
        ElasticPagedList<Map<String,Object>> result =  logDao.search(search, page);
        return result;
    }

}
