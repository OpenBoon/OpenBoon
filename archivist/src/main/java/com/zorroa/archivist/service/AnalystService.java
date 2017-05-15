package com.zorroa.archivist.service;

import com.zorroa.common.domain.Analyst;
import com.zorroa.common.domain.AnalystSpec;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
public interface AnalystService {

    PagedList<Analyst> getAll(Pager paging);

    void register(AnalystSpec spec);

    Analyst get(String url);

    int getCount();

    List<Analyst> getActive();
}
