package com.zorroa.archivist.web.gui;

import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.service.PluginService;
import com.zorroa.sdk.domain.Pager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Created by chambers on 8/17/16.
 */
@Controller
public class PluginGuiController {

    private static final Logger logger = LoggerFactory.getLogger(PluginGuiController.class);

    @Autowired
    PluginService pluginService;

    @RequestMapping("/admin/gui/plugins")
    public String plugins(Model model, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        model.addAttribute("plugins", pluginService.getAllPlugins(new Pager(page)));
        return "plugins";
    }

    @RequestMapping("/admin/gui/plugins/{id}")
    public String plugins(Model model, @PathVariable UUID id) {
        Plugin plugin = pluginService.getPlugin(id);
        standardModel(model);
        model.addAttribute("plugin", plugin);
        model.addAttribute("processors", pluginService.getAllProcessors(plugin));
        return "plugin";
    }

    @RequestMapping("/admin/gui/plugins/{pid}/processors/{id}")
    public String plugins(Model model, @PathVariable UUID pid, @PathVariable UUID id) {
        standardModel(model);
        Plugin plugin = pluginService.getPlugin(pid);

        model.addAttribute("plugin", plugin);
        model.addAttribute("processor", pluginService.getProcessor(id));
        return "processor";
    }

    @RequestMapping("/admin/gui/docs/processor/{name:.+}")
    public String processor_docs(Model model, @PathVariable String name) {
        model.addAttribute("processor", pluginService.getProcessor(name));
        return "processor_docs";
    }

    /**
     * Supports the data necessary for the overall template. (layout.html)
     * @param model
     */
    private void standardModel(Model model) {
    }

}
