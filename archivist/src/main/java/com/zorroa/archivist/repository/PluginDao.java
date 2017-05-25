package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Plugin;
import com.zorroa.sdk.plugins.PluginSpec;

import java.util.Map;

/**
 * Created by chambers on 8/16/16.
 */
public interface PluginDao extends GenericNamedDao<Plugin, PluginSpec> {

    /**
     * Get a map of plugin name/md5 sum for quickly checking if a plugin
     * version is installed or not.
     *
     * @return
     */
    Map<String, String> getInstalledVersions();

    boolean update(int id, PluginSpec spec);
}
