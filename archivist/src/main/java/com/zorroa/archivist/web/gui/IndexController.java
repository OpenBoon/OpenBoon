package com.zorroa.archivist.web.gui;

import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.archivist.domain.PermissionSpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.*;
import com.zorroa.common.domain.EventSearch;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.EventLogDao;
import com.zorroa.sdk.plugins.ModuleRef;
import com.zorroa.sdk.search.AssetSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * Created by chambers on 6/3/16.
 */
@Controller
public class IndexController {

    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    PipelineService pipelineService;

    @Autowired
    IngestService ingestService;

    @Autowired
    JobService jobService;

    @Autowired
    ImportService importService;

    @Autowired
    AnalystService analystService;

    @Autowired
    PluginService pluginService;

    @Autowired
    FolderService folderService;

    @Autowired
    SearchService searchService;

    @Autowired
    AssetService assetService;

    @Autowired
    UserService userService;

    @Autowired
    HealthEndpoint healthEndpoint;

    @Autowired
    EventLogDao eventLogDao;

    @Autowired
    Validator validator;

    @RequestMapping("/")
    public String index() {
        return "redirect:/gui";
    }

    @RequestMapping("/gui")
    public String index(Model model) {
        standardModel(model);
        model.addAttribute("assetCount", searchService.count(new AssetSearch()));
        model.addAttribute("userCount", userService.getCount());
        model.addAttribute("folderCount", folderService.count());
        model.addAttribute("analystCount", analystService.getCount());
        model.addAttribute("user", SecurityUtils.getUser());
        return "overview";
    }

    @RequestMapping("/gui/login")
    public String login(){
        return "login";
    }

    @RequestMapping("/gui/logout")
    public String logout(HttpServletRequest req) throws ServletException {
        req.logout();
        return "login?logout";
    }

    @RequestMapping("/gui/permissions")
    public String permissions(Model model,
                              @RequestParam(value="page", required=false) Integer page,
                              @RequestParam(value="count", required=false) Integer count) {

        Paging paging = new Paging(page, count);
        standardModel(model);
        model.addAttribute("page", paging);
        model.addAttribute("perms", userService.getPermissions(paging));
        model.addAttribute("permSpec", new PermissionSpec());
        return "permissions";
    }

    @RequestMapping(value="/gui/permissions", method=RequestMethod.POST)
    public String permissions(Model model, @ModelAttribute("permSpec") PermissionSpec permSpec,
                              BindingResult bindingResult) {
        standardModel(model);
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            return "permissions";
        }
        else {
            userService.createPermission(permSpec);
            return "redirect:/gui/permissions";
        }
    }

    @RequestMapping("/gui/permissions/{id}")
    public String getPermission(Model model, @PathVariable int id) {
        standardModel(model);
        model.addAttribute("perm", userService.getPermission(id));
        model.addAttribute("permSpec", new PermissionSpec());
        return "permission";
    }

    @RequestMapping("/gui/assets")
    public String assets(Model model,
                         @RequestParam(value="page", required=false) Integer page,
                         @RequestParam(value="count", required=false) Integer count, @RequestParam(value="query", required=false) String query) {

        standardModel(model);
        Paging paging = new Paging(page, count);
        model.addAttribute("page", paging);
        model.addAttribute("assets", searchService.getAll(paging,
                new AssetSearch(query)));
        return "assets";
    }

    @RequestMapping("/gui/fields")
    public String fields(Model model) {
        standardModel(model);
        model.addAttribute("fields", searchService.getFields());
        return "fields";
    }


    @RequestMapping("/gui/analysts")
    public String analysts(Model model,
                           @RequestParam(value="page", required=false) Integer page,
                           @RequestParam(value="count", required=false) Integer count) {
        standardModel(model);
        Paging paging = new Paging(page, count);
        model.addAttribute("page", paging);
        model.addAttribute("analysts", analystService.getAll(paging));
        return "analysts";
    }

    @RequestMapping("/gui/ingests")
    public String getIngests(Model model) {
        standardModel(model);
        model.addAttribute("pipelines", pipelineService.getAll());
        model.addAttribute("ingests", ingestService.getAll());
        model.addAttribute("ingestForm", new NewIngestForm());
        return "ingests";
    }

    @RequestMapping(value="/gui/ingests", method=RequestMethod.POST)
    public String createIngest(Model model,
                               @Valid @ModelAttribute("ingestForm") NewIngestForm ingestForm,
                               BindingResult bindingResult) {
        standardModel(model);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            model.addAttribute("pipelines", pipelineService.getAll());
            return "ingests";
        }

        model.addAttribute("ingests", ingestService.getAll());
        model.addAttribute("ingestForm", new NewIngestForm());

        IngestSpec spec = new IngestSpec();
        spec.setName(ingestForm.getName());
        spec.setSchedule(ingestForm.getSchedule());
        spec.setRunNow(ingestForm.isRunNow());
        spec.setAutomatic(ingestForm.isAutomatic());
        spec.setFolderId(ingestForm.getFolderId());
        spec.setPipelineId(ingestForm.getPipelineId());

        for (String path: ingestForm.getPaths()) {
            ModuleRef gen = new ModuleRef("generator:zorroa-core:SharedVolume");
            gen.setArg("path", path);
            spec.addToGenerators(gen);
        }

        ingestService.create(spec);

        return "redirect:/gui/ingests";
    }

    @RequestMapping("/gui/pipelines")
    public String pipelines(Model model) {
        standardModel(model);
        model.addAttribute("pipelines", pipelineService.getAll());
        return "pipelines";
    }

    @RequestMapping("/gui/plugins")
    public String plugins(Model model, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        model.addAttribute("plugins", pluginService.getPlugins(new Paging(page)));
        return "plugins";
    }

    @RequestMapping("/gui/plugins/{name}")
    public String plugins(Model model, @PathVariable String name) {
        standardModel(model);
        model.addAttribute("plugin", pluginService.get(name));
        model.addAttribute("modules", pluginService.getModules(name));
        return "plugin";
    }

    @RequestMapping("/gui/plugins/{name}/module/{id}")
    public String plugins(Model model, @PathVariable String name, @PathVariable String id) {
        standardModel(model);
        model.addAttribute("plugin", pluginService.get(name));
        model.addAttribute("module", pluginService.getModule(id));
        return "module";
    }

    @RequestMapping("/gui/status")
    public String status(Model model) {
        standardModel(model);
        Health health = healthEndpoint.invoke();
        model.addAttribute("health", health.getDetails());
        model.addAttribute("status", health.getDetails());
        return "status";
    }

    @RequestMapping("/gui/events")
    public String events(Model model,
                         @RequestParam(value="type", required=false) String type,
                         @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        /**
         * TODO: need to handle multiple types.
         */
        EventSearch search = new EventSearch();
        if (type != null) {
            search.setObjectTypes(ImmutableSet.of(type));
        }
        model.addAttribute("search", search);
        model.addAttribute("events", eventLogDao.getAll(search, new Paging(page)));
        return "events";
    }

    /**
     * Supports the data necessary for the overall template. (layout.html)
     * @param model
     */
    private void standardModel(Model model) {
        //model.addAttribute("stdJobs", ingestService.getAllIngests(IngestState.Running, 10));
    }
}
