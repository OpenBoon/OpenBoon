package com.zorroa.archivist.web.gui;

import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.service.PluginService;
import com.zorroa.common.domain.Paging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by chambers on 8/17/16.
 */
@Controller
public class PluginGuiController {

    @Autowired
    PluginService pluginService;

    @RequestMapping("/gui/plugins")
    public String plugins(Model model, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        model.addAttribute("plugins", pluginService.getAllPlugins(new Paging(page)));
        return "plugins";
    }

    @RequestMapping("/gui/plugins/{id}")
    public String plugins(Model model, @PathVariable int id) {
        Plugin plugin = pluginService.getPlugin(id);
        standardModel(model);
        model.addAttribute("plugin", plugin);
        model.addAttribute("processors", pluginService.getAllProcessors(plugin));
        return "plugin";
    }

    @RequestMapping("/gui/plugins/{pid}/processors/{id}")
    public String plugins(Model model, @PathVariable int pid, @PathVariable int id) {
        standardModel(model);
        Plugin plugin = pluginService.getPlugin(id);

        model.addAttribute("plugin", plugin);
        model.addAttribute("processor", pluginService.getProcessor(id));
        return "processor";
    }

    /**
     * Supports the data necessary for the overall template. (layout.html)
     * @param model
     */
    private void standardModel(Model model) {
    }

}
