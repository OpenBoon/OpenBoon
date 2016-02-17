package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.client.analyst.AnalystClient;
import com.zorroa.archivist.sdk.domain.Analyst;
import com.zorroa.archivist.sdk.domain.AnalystPing;

import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
public interface AnalystService {
    void register(AnalystPing ping);

    void shutdown(AnalystPing ping);

    List<Analyst> getAll();

    AnalystClient getAnalystClient() throws Exception;
}
