package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Filter;
import com.zorroa.archivist.domain.FilterSpec;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;

/**
 * Created by chambers on 8/9/16.
 */
public interface FilterService {

    Filter create(FilterSpec spec);

    List<Filter> getAll();

    PagedList<Filter> getPaged(Pager page);
}
