package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

public interface LogService {

    @Async
    void log(LogSpec spec);

    PagedList<Map<String,Object>> search(LogSearch search, Pager page);
}
