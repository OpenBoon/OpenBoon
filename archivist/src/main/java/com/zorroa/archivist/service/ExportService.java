package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.ExportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

/**
 * Created by chambers on 11/1/15.
 */
public interface ExportService {

    Job create(ExportSpec spec);

    PagedList<Job> getAll(Pager page);
}
