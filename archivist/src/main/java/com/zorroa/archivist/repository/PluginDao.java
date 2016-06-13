package com.zorroa.archivist.repository;

import com.zorroa.sdk.plugins.PluginProperties;
import com.zorroa.sdk.processor.ProcessorProperties;
import com.zorroa.sdk.processor.ProcessorType;

import java.util.List;

/**
 * Created by chambers on 5/20/16.
 */
public interface PluginDao {

    int create(PluginProperties plugin);

    void addProcessor(int pluginId, ProcessorProperties processor);

    List<PluginProperties> getPlugins();

    List<ProcessorProperties> getProcessors(int plugin);

    List<ProcessorProperties> getProcessors(ProcessorType type);

    List<ProcessorProperties> getProcessors();
}
