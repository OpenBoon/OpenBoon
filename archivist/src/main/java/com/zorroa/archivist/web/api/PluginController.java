package com.zorroa.archivist.web.api;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.service.PluginService;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.exception.PluginException;
import com.zorroa.sdk.plugins.Module;
import com.zorroa.sdk.plugins.Plugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Created by chambers on 6/29/16.
 */

@RestController
public class PluginController {

    @Autowired
    ApplicationProperties properties;

    @Autowired
    PluginService pluginService;

    @RequestMapping(value="/api/v1/plugins", method = RequestMethod.POST)
    public Object handlePluginUpload(@RequestParam("file") MultipartFile file) {
        if (!file.isEmpty()) {
            if (!file.getOriginalFilename().endsWith("-plugin.zip")) {
                throw new RuntimeException("The plugin package name must end with -plugin.zip");
            }
            Plugin p = pluginService.installPlugin(file);
            return ImmutableMap.of(
                    "name",p.getName(),
                    "description", p.getDescription(),
                    "version", p.getVersion(),
                    "publisher", p.getPublisher(),
                    "language", p.getLanguage());
        }
        else {
            throw new PluginException("Failed to handle plugin zip file");
        }
    }

    @RequestMapping(value="/api/v1/plugins/{plugin}/module/{type}", method=RequestMethod.GET)
    public List<Module> modules(@PathVariable String plugin, @PathVariable String type) {
        if (plugin.equals("_all")) {
            return pluginService.getModules(null, type);
        } else {
            return pluginService.getModules(plugin, type);
        }
    }
}
