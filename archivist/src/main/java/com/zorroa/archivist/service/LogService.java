package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.PagedElasticList;
import org.springframework.scheduling.annotation.Async;

public interface LogService {

    @Async
    void log(LogSpec spec);

    PagedElasticList search(LogSearch search, Paging page);
}
