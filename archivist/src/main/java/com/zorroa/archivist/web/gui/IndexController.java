package com.zorroa.archivist.web.gui;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.archivist.domain.UserUpdate;
import com.zorroa.archivist.repository.EventLogDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.*;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.SerializableElasticResult;
import com.zorroa.sdk.domain.AssetSearch;
import com.zorroa.sdk.domain.EventLogSearch;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
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
import java.util.Map;

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
    public String assets(Model model) {
        standardModel(model);
        model.addAttribute("search", new AssetSearch());
        return "assets";
    }

    @RequestMapping(value="/gui/assets", method=RequestMethod.POST)
    public String assetsSearch(@ModelAttribute AssetSearch search, Model model) {
        standardModel(model);
        model.addAttribute("search", new AssetSearch());

        List<Map<String, Object>> result = Lists.newArrayList();
        for (SearchHit hit: searchService.search(search).getHits().getHits()) {
            result.add(hit.sourceAsMap());
        }

        model.addAttribute("result", result);
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
        standardModel(model);
        Folder folder = new Folder();
        folder.setName("/");
        model.addAttribute("path", Lists.newArrayList(folder));
        model.addAttribute("folder", folder);
        model.addAttribute("parent", null);
        model.addAttribute("children", folderService.getAll());
        return "folders";
    }

    @RequestMapping("/gui/folders/{id}")
    public String folders(Model model, @PathVariable int id) {
        standardModel(model);
        Folder folder = folderService.get(id);
        model.addAttribute("folder", folder);
        model.addAttribute("parent", folderService.get(folder.getParentId()));
        model.addAttribute("children", folderService.getChildren(folder));

        List<Folder> path = Lists.newArrayList(folder);
        Folder parent = folderService.get(folder.getParentId());
        while (parent.getParentId() != null) {
            path.add(parent);
            parent = folderService.get(parent.getParentId());
        }
        Collections.reverse(path);
        model.addAttribute("path", path);
        return "folders";
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
        model.addAttribute("generators", pluginService.getModules(null, "generator"));
        model.addAttribute("spec", new IngestSpec());
        return "ingests";
    }

    @RequestMapping(value="/gui/ingests",  method=RequestMethod.POST)
    public String createIngest(Model model,
                               @Valid @ModelAttribute("ingestSpec") IngestSpec spec,
                               BindingResult bindingResult) {
        logger.info("{}", spec);
        standardModel(model);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            return "ingests";
        }

        model.addAttribute("ingests", ingestService.getAll());
        model.addAttribute("spec", new IngestSpec());
        ingestService.create(spec);
        return "redirect:/gui/ingests";
    }

    @RequestMapping("/gui/imports/{id}")
    public String ingest(Model model, @PathVariable int id) {
        standardModel(model);
        //model.addAttribute("ingest", ingestService.getIngest(id));
        //model.addAttribute("ingestUpdateBuilder", new IngestUpdateBuilder());
        return "ingest";
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
        model.addAttribute("importors", pluginService.getModules(name, "import"));
        model.addAttribute("generators", pluginService.getModules(name, "generator"));
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
                         @RequestParam(value="type", required=false) String type) {
        standardModel(model);
        /**
         * TODO: need to handle multiple types.
         */
        EventLogSearch search = new EventLogSearch();
        if (type != null) {
            search.setTypes(ImmutableSet.of(type));
        }

        model.addAttribute("search", search);
        SearchResponse rsp =  eventLogDao.getAll(search);
        model.addAttribute("events", new SerializableElasticResult(rsp));
        return "events";
    }

    /**
     * Supports the data necessary for the overall template. (layout.html)
     * @param model
     */
    private void standardModel(Model model) {
        SerializableElasticResult result = new SerializableElasticResult(
                eventLogDao.getAll(new EventLogSearch()
                        .setLimit(0)
                        .setAfterTime(System.currentTimeMillis() - (86400 * 1000))));

        model.addAttribute("stdEvents", result);
        //model.addAttribute("stdJobs", ingestService.getAllIngests(IngestState.Running, 10));
    }
}
