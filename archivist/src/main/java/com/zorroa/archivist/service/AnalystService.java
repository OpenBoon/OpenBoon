package com.zorroa.archivist.service;

import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.client.analyst.AnalystClient;
import com.zorroa.sdk.domain.Analyst;

import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
public interface AnalystService {
    List<Analyst> getAll();

    List<Analyst> getAll(Paging paging);

    Analyst get(String url);

    int getCount();

    List<Analyst> getActive();

    AnalystClient getAnalystClient();
}
