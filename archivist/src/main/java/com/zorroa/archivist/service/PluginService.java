package com.zorroa.archivist.service;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.plugins.Module;
import com.zorroa.sdk.plugins.Plugin;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by chambers on 6/28/16.
 */
public interface PluginService {

    Plugin registerPlugin(MultipartFile file);

    Plugin registerPlugin(Path zipFilePath);

    PagedList<List<Plugin>> getPlugins(Paging page);

    List<Plugin> getPlugins();

    Plugin get(String name);

    List<Module> getModules(String plugin);

    List<Module> getModules(String plugin, String type);

    Module getModule(String plugin, String name);

    List<Module> getModules();

    void registerAllPlugins();
}
