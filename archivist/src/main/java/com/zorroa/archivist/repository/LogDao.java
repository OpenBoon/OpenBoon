package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.Map;

/**
 * Created by chambers on 12/28/15.
 */
public interface LogDao {


    void create(LogSpec spec);

    PagedList<Map<String,Object>> search(LogSearch search, Pager page);

    String getIndexName(LogSpec spec);
}
