package com.zorroa.archivist.service;

import com.zorroa.sdk.client.analyst.AnalystClient;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystPing;
import com.zorroa.sdk.plugins.PluginProperties;
import com.zorroa.sdk.processor.ProcessorProperties;
import com.zorroa.sdk.processor.ProcessorType;

import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
public interface AnalystService {
    void register(AnalystPing ping);

    void shutdown(AnalystPing ping);

    List<Analyst> getAll();

    AnalystClient getAnalystClient() throws Exception;

    Analyst get(String url);

    int getCount();

    List<Analyst> getActive();

    List<PluginProperties> getPlugins();

    List<ProcessorProperties> getProcessors(ProcessorType type);

    List<ProcessorProperties> getProcessors();
}
