package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;

/**
 * Created by chambers on 7/11/16.
 */
public interface ImportService {

    PagedList<Job> getAll(Paging page);

    /**
     * Create a import job with the given import spec.
     *
     * @param spec
     * @return
     */
    Job create(ImportSpec spec);
}
