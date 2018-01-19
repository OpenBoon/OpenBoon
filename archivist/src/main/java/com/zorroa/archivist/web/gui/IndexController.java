package com.zorroa.archivist.web.gui;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.*;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.PipelineType;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
    AnalystService analystService;

    @Autowired
    PluginService pluginService;

    @Autowired
    FolderService folderService;

    @Autowired
    EventLogService logService;

    @Autowired
    SearchService searchService;

    @Autowired
    UserService userService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    HealthEndpoint healthEndpoint;

    @Autowired
    ApplicationProperties properties;

    @RequestMapping(value="/docs", method=RequestMethod.GET)
    public String docIndex(Model model) throws IOException {

        Path root = properties.getPath("archivist.path.docs");
        List<String> docs = Lists.newArrayList();
        for (File f: root.toFile().listFiles()) {
            docs.add(f.getName());
        }
        model.addAttribute("docs", docs);
        return "docs";
    }

    @RequestMapping("/admin/gui")
    public String index(Model model) {
        logger.info("yay!");
        standardModel(model);
        model.addAttribute("assetCount", searchService.count(new AssetSearch()));
        model.addAttribute("userCount", userService.getCount());
        model.addAttribute("folderCount", folderService.count());
        model.addAttribute("analystCount", analystService.getCount());
        model.addAttribute("user", SecurityUtils.getUser());
        return "overview";
    }

    @RequestMapping("/admin/gui/permissions")
    public String permissions(Model model,
                              @RequestParam(value="page", required=false) Integer page,
                              @RequestParam(value="count", required=false) Integer count) {

        Pager paging = new Pager(page, count);
        standardModel(model);
        model.addAttribute("page", paging);
        model.addAttribute("perms", permissionService.getPermissions(paging));
        model.addAttribute("permSpec", new PermissionSpec());
        return "permissions";
    }

    @RequestMapping(value="/admin/gui/permissions", method=RequestMethod.POST)
    public String permissions(Model model, @ModelAttribute("permSpec") PermissionSpec permSpec,
                              BindingResult bindingResult) {
        standardModel(model);
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            return "permissions";
        }
        else {
            permissionService.createPermission(permSpec);
            return "redirect:/admin/gui/permissions";
        }
    }

    @RequestMapping("/admin/gui/permissions/{id}")
    public String getPermission(Model model, @PathVariable int id) {
        standardModel(model);
        model.addAttribute("perm", permissionService.getPermission(id));
        model.addAttribute("permSpec", new PermissionSpec());
        return "permission";
    }

    @RequestMapping("/admin/gui/assets")
    public String assets(Model model,
                         @RequestParam(value="page", required=false) Integer page,
                         @RequestParam(value="count", required=false) Integer count, @RequestParam(value="query", required=false) String query) {

        standardModel(model);
        Pager paging = new Pager(page, count);
        model.addAttribute("page", paging);
        model.addAttribute("assets", searchService.search(paging,
                new AssetSearch(query)));
        return "assets";
    }

    @RequestMapping("/admin/gui/fields")
    public String fields(Model model) {
        standardModel(model);
        model.addAttribute("fields", searchService.getFields("asset"));
        return "fields";
    }

    @RequestMapping("/admin/gui/analysts")
    public String analysts(Model model,
                           @RequestParam(value="page", required=false) Integer page,
                           @RequestParam(value="count", required=false) Integer count) {
        standardModel(model);
        Pager paging = new Pager(page, count);
        model.addAttribute("page", paging);
        model.addAttribute("analysts", analystService.getAll(paging));
        return "analysts";
    }

    @RequestMapping("/admin/gui/pipelines")
    public String pipelines(Model model) {
        standardModel(model);
        model.addAttribute("pipelines", pipelineService.getAll());
        model.addAttribute("pipelineSpec", new PipelineSpecV());
        return "pipelines";
    }


    @RequestMapping("/admin/gui/pipelines/{id}")
    public String getPipeline(Model model, @PathVariable int id) {
        standardModel(model);

        Pipeline pipeline = pipelineService.get(id);

        model.addAttribute("pipeline", pipelineService.get(id));
        model.addAttribute("processors", pluginService.getAllProcessors(
                new ProcessorFilter().setTypes(PipelineType.ALLOWED_PROCESSOR_TYPES.get(pipeline.getType()))));


        model.addAttribute("pipelineSpec", new PipelineSpecV());
        return "pipeline";
    }

    @RequestMapping(value="/admin/gui/pipelines", method=RequestMethod.POST)
    public String createPipeline(Model model,
                                 @Valid @ModelAttribute("pipelineSpec") PipelineSpecV pipelineSpec,
                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pipelines", pipelineService.getAll());
            return "pipelines";
        }
        else {
            Pipeline p = pipelineService.create(pipelineSpec);
            return "redirect:/admin/gui/pipelines/" + p.getId();
        }
    }

    @RequestMapping(value="/admin/gui/pipelines/_import", method=RequestMethod.POST, consumes = "multipart/form-data")
    public String uploadPipeline(@RequestParam("jfile") MultipartFile jfile, RedirectAttributes redirectAttributes) throws IOException {

        PipelineSpecV spec = Json.Mapper.readValue(jfile.getInputStream(), PipelineSpecV.class);
        int nameCounter = 1;
        String checkName = spec.getName();
        for(;;) {
            if (pipelineService.exists(checkName)) {
                checkName = "Imported(" + nameCounter + ") " + spec.getName();
                nameCounter++;
            }
            else {
                spec.setName(checkName);
                break;
            }
        }
        Pipeline p = pipelineService.create(spec);
        redirectAttributes.addFlashAttribute("message",
                "Pipeline imported");
        return "redirect:/admin/gui/pipelines/" + p.getId();
    }


    @RequestMapping("/admin/gui/status")
    public String status(Model model) {
        standardModel(model);
        Health health = healthEndpoint.invoke();
        model.addAttribute("health", health.getDetails());
        model.addAttribute("status", health.getDetails());
        return "status";
    }

    @RequestMapping("/admin/gui/logs")
    public String events(Model model,
                         @RequestParam(value="query", required=false) String query,
                         @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);

        EventLogSearch search = new EventLogSearch();
        if (query != null) {
            search.setQuery(parseQueryParam(query));
        }

        search.setAggs(ImmutableMap.of("all",
                ImmutableMap.of("global", ImmutableMap.of(),
                        "aggs", ImmutableMap.of("actions",
                                ImmutableMap.of("terms",ImmutableMap.of("field", "action"))))));

        Pager paging = new Pager(page);
        search.setFrom(paging.getFrom());
        search.setSize(paging.getSize());

        PagedList<Map<String, Object>> logs =logService.getAll("user", search);

        model.addAttribute("search", search);
        model.addAttribute("logs",logs);
        model.addAttribute("page", logs.getPage());
        model.addAttribute("query", query);
        return "logs";
    }

    /**
     * Very basic conversion from a query string to an elastic query. We could actually
     * do this way cooler.
     *
     * @param query
     * @return
     */
    private Map<String, Map<String, Object>> parseQueryParam(String query) {
        if (query == null || query.isEmpty()) {
            return EventLogSearch.DEFAULT_QUERY;
        }
        Map<String, Map<String, Object>> result = Maps.newHashMap();
        for (String phrase : Splitter.on(",").trimResults().omitEmptyStrings().split(query)) {
            String[] parts = phrase.split(":");
            if (parts.length != 3) {
                continue;
            }
            String type = parts[0];
            String field = parts[1];
            String value = parts[2];

            Map<String, Object> g = result.get(type);
            if (g == null) {
                g = Maps.newHashMap();
                result.put(type, g);
            }
            g.put(field, value);
        }
        return result;
    }

    /**
     * Supports the data necessary for the overall template. (layout.html)
     * @param model
     */
    private void standardModel(Model model) {
        //model.addAttribute("stdJobs", ingestService.getAllIngests(IngestState.Running, 10));
    }
}
