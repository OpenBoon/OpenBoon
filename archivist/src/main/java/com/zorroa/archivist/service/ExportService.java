package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.ExportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Export;

/**
 * Created by chambers on 11/1/15.
 */
public interface ExportService {

    Job create(ExportSpec spec);

    Export get(int id);

    PagedList<Job> getAll(Paging page);
}
