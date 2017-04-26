package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Acl;
import com.zorroa.archivist.domain.Filter;
import com.zorroa.archivist.domain.FilterSpec;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;

/**
 * Created by chambers on 8/9/16.
 */
public interface FilterService {

    Filter create(FilterSpec spec);

    Acl getMatchedAcls(Document doc);

    void applyPermissionSchema(Document doc);

    List<Filter> getAll();

    Filter get(int id);

    boolean delete(Filter filter);

    boolean setEnabled(Filter filter, boolean value);

    PagedList<Filter> getPaged(Pager page);
}
