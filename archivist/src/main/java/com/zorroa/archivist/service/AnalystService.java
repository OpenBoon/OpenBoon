package com.zorroa.archivist.service;

import com.zorroa.archivist.AnalystClient;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
public interface AnalystService {

    PagedList<Analyst> getAll(Pager paging);

    Analyst get(String url);

    int getCount();

    List<Analyst> getActive();

    AnalystClient getAnalystClient(String host);

    AnalystClient getAnalystClient();
}
