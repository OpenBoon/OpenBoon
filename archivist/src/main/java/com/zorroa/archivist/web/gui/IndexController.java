package com.zorroa.archivist.web.gui;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
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
import java.util.Collections;
import java.util.List;

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
        model.addAttribute("assetCount", searchService.count(new AssetSearch()).getCount());
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
    public String permissions(Model model) {
        standardModel(model);
        model.addAttribute("allPermissions", userService.getPermissions());
        return "permissions";
    }

    @RequestMapping("/gui/users")
    public String users(Model model, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        /**
         * TODO: generalize table paging so more types can use it.
         */
        page = Math.max(1, page == null ? 1: page);
        int limit = 10;
        int offset = (page - 1) * limit;
        int count = userService.getCount();
        int maxPages = (count / limit) + 1;

        model.addAttribute("prevPage", Math.max(1, page-1));
        model.addAttribute("nextPage", Math.min(maxPages, page+1));
        model.addAttribute("pageDisplay", String.format("%d of %d", page, maxPages));
        model.addAttribute("allUsers", userService.getAll(limit, offset));
        model.addAttribute("userBuilder", new UserSpec());
        return "users";
    }

    @RequestMapping("/gui/users/{id}")
    public String user(Model model, @PathVariable int id) {
        standardModel(model);
        model.addAttribute("user", userService.get(id));
        model.addAttribute("userUpdateBuilder", new UserSpec());
        return "user";
    }

    @RequestMapping(value="/gui/users/{id}", method=RequestMethod.POST)
    public String updateUser(Model model, @PathVariable int id,
                             @Valid @ModelAttribute("userUpdateBuilder") UserUpdate userUpdateBuilder,
                             BindingResult bindingResult) {
        standardModel(model);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.get(id));
            model.addAttribute("errors", true);
            return "user";
        }
        else {
            userService.update(userService.get(id), userUpdateBuilder);
        }

        model.addAttribute("user", userService.get(id));
        model.addAttribute("userUpdateBuilder", new UserUpdate());
        return "user";
    }

    @RequestMapping(value="/gui/users", method=RequestMethod.POST)
    public String createUser(Model model,
                             @Valid @ModelAttribute("userBuilder") UserSpec userBuilder,
                             BindingResult bindingResult) {
        standardModel(model);
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            return "users";
        }

        model.addAttribute("allUsers", userService.getAll());
        User user = userService.create(userBuilder);
        return "redirect:/gui/users/"+ user.getId();
    }

    @RequestMapping("/gui/assets")
    public String assets(Model model,
                         @RequestParam(value="page", required=false) Integer page,
                         @RequestParam(value="count", required=false) Integer count, @RequestParam(value="query", required=false) String query) {

        standardModel(model);
        model.addAttribute("assets", searchService.getAll(new Paging(page, count),
                new AssetSearch(query)));
        return "assets";
    }

    @RequestMapping("/gui/fields")
    public String fields(Model model) {
        standardModel(model);
        model.addAttribute("fields", searchService.getFields());
        return "fields";
    }

    @RequestMapping("/gui/folders")
    public String folders(Model model) {
        return "redirect:/gui/folders/"+ Folder.ROOT_ID;
    }

    @RequestMapping("/gui/folders/{id}")
    public String folders(Model model, @PathVariable int id) {
        standardModel(model);
        folderModel(model, folderService.get(id));
        model.addAttribute("folderSpec", new FolderSpec());
        return "folders";
    }

    @RequestMapping(value="/gui/folders/{id}", method=RequestMethod.POST)
    public String createFolder(Model model, @PathVariable int id,
                          @Valid @ModelAttribute("folderSpec") FolderSpec folderSpec,
                          BindingResult bindingResult) {
        standardModel(model);
        folderModel(model, folderService.get(id));

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            logger.warn("errors: {}", bindingResult.getAllErrors());
            return "folders";
        }
        else {
            logger.info("creating folder");
            folderService.create(folderSpec);
            return "redirect:/gui/folders/" + id;
        }
    }

    private void folderModel(Model model, Folder folder) {
        model.addAttribute("folder", folder);
        model.addAttribute("children", folderService.getChildren(folder));

        if (folder.getParentId() == null) {
            model.addAttribute("parent", null);
        }
        else {
            model.addAttribute("parent", folderService.get(folder.getParentId()));
            List<Folder> path = Lists.newArrayList(folder);
            Folder parent = folderService.get(folder.getParentId());
            while (parent.getParentId() != null) {
                path.add(parent);
                parent = folderService.get(parent.getParentId());
            }
            Collections.reverse(path);
            model.addAttribute("path", path);
        }

    }

    @RequestMapping("/gui/analysts")
    public String analysts(Model model,
                           @RequestParam(value="page", required=false) Integer page,
                           @RequestParam(value="count", required=false) Integer count) {
        standardModel(model);
        model.addAttribute("analysts", analystService.getAll(new Paging(page, count)));
        return "analysts";
    }

    @RequestMapping("/gui/ingests")
    public String imports(Model model) {
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

    @RequestMapping("/gui/imports/{id}")
    public String ingest(Model model, @PathVariable int id, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        model.addAttribute("imp", jobService.get(id));
        model.addAttribute("assets", assetService.getAll(new Paging(page)));
        //model.addAttribute("ingestUpdateBuilder", new IngestUpdateBuilder());
        return "import";
    }

    @RequestMapping("/gui/imports")
    public String imports(Model model, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        model.addAttribute("imports", importService.getAll(new Paging(page)));
        return "imports";
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
