package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.Map;

public interface LogService {

    void log(LogSpec spec);

    void logAsync(LogSpec spec);

    PagedList<Map<String,Object>> search(LogSearch search, Pager page);
}
