package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.ElasticPagedList;

import java.util.Map;

/**
 * Created by chambers on 12/28/15.
 */
public interface LogDao {


    void create(LogSpec spec);

    ElasticPagedList<Map<String,Object>> search(LogSearch search, Paging page);

    String getIndexName(LogSpec spec);
}
