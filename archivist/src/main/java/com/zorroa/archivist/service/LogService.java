package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.ElasticPagedList;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

public interface LogService {

    @Async
    void log(LogSpec spec);

    ElasticPagedList<Map<String,Object>> search(LogSearch search, Paging page);
}
