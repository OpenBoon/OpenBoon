package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.ExportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;

/**
 * Created by chambers on 11/1/15.
 */
public interface ExportService {

    Job create(ExportSpec spec);

    PagedList<Job> getAll(Paging page);
}
