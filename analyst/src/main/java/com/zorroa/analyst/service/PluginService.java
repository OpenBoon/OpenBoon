package com.zorroa.analyst.service;

import com.zorroa.analyst.domain.PluginProperties;
import com.zorroa.sdk.domain.Tuple;
import com.zorroa.sdk.plugins.Plugin;
import com.zorroa.sdk.processor.ingest.IngestProcessor;

import java.util.List;

/**
 * Created by chambers on 4/26/16.
 */
public interface PluginService {

    List<Tuple<PluginProperties, Plugin>> getLoadedPlugins();

    IngestProcessor getIngestProcessor(String name) throws Exception;

    void loadPlugins();

}
