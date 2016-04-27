package com.zorroa.analyst.service;

import com.zorroa.analyst.domain.PluginProperties;
import com.zorroa.archivist.sdk.domain.Tuple;
import com.zorroa.archivist.sdk.plugins.Plugin;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;

import java.util.List;

/**
 * Created by chambers on 4/26/16.
 */
public interface PluginService {

    List<Tuple<PluginProperties, Plugin>> getLoadedPlugins();

    IngestProcessor getIngestProcessor(String name) throws Exception;

}
