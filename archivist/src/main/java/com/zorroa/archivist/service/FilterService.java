package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Filter;
import com.zorroa.archivist.domain.FilterSpec;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;

import java.util.List;

/**
 * Created by chambers on 8/9/16.
 */
public interface FilterService {

    Filter create(FilterSpec spec);

    List<Filter> getAll();

    PagedList<Filter> getPaged(Paging page);
}
