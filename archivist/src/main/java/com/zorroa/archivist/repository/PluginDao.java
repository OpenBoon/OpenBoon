package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Plugin;
import com.zorroa.sdk.plugins.PluginSpec;

/**
 * Created by chambers on 8/16/16.
 */
public interface PluginDao extends GenericNamedDao<Plugin, PluginSpec> {

    boolean update(int id, PluginSpec spec);
}
