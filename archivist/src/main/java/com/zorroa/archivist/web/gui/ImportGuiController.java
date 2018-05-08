package com.zorroa.archivist.web.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.service.ImportService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.archivist.service.PipelineService;
import com.zorroa.archivist.service.PluginService;
import com.zorroa.archivist.web.gui.forms.SearchImportForm;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.PipelineType;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.File;
import java.util.List;
import java.util.UUID;

import static com.zorroa.archivist.security.UtilsKt.getUsername;

/**
 * Created by chambers on 7/28/16.
 */
@Controller
public class ImportGuiController {

    private static final Logger logger = LoggerFactory.getLogger(ImportGuiController.class);

    @Autowired
    PipelineService pipelineService;

    @Autowired
    JobService jobService;

    @Autowired
    ImportService importService;

    @Autowired
    PluginService pluginService;

    @RequestMapping("/admin/gui/imports/{id}")
    public String getImport(Model model, @PathVariable UUID id, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        Pager paging = new Pager(page);
        model.addAttribute("page", paging);
        model.addAttribute("job", jobService.get(id));
        model.addAttribute("tasks", jobService.getAllTasks(id, paging));
        model.addAttribute("serverImportForm", new ServerImportForm());
        model.addAttribute("searchImportForm", new SearchImportForm());
        model.addAttribute("pipelines", pipelineService.getAll(PipelineType.Import));
        return "import";
    }

    @RequestMapping("/admin/gui/imports")
    public String getAllImports(Model model, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        Pager paging = new Pager(page);
        model.addAttribute("page", paging);
        model.addAttribute("imports", importService.getAll(paging));
        model.addAttribute("pipelines", pipelineService.getAll(PipelineType.Import));
        model.addAttribute("serverImportForm", new ServerImportForm());
        model.addAttribute("searchImportForm", new SearchImportForm());
        return "imports";
    }

    @RequestMapping(value="/admin/gui/imports/server",  method= RequestMethod.POST)
    public String createImport(Model model,
                               @Valid @ModelAttribute("serverImportForm") ServerImportForm serverImportForm, BindingResult bindingResult) {

        standardModel(model);
        Pager paging = new Pager(1);
        model.addAttribute("page", paging);
        model.addAttribute("imports", importService.getAll(paging));
        model.addAttribute("pipelines", pipelineService.getAll(PipelineType.Import));
        model.addAttribute("searchImportForm", new SearchImportForm());
        model.addAttribute("serverImportForm", new ServerImportForm());

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            return "imports";
        }

        ImportSpec spec = new ImportSpec();
        spec.setName(serverImportForm.getName());
        spec.setProcessors(ImmutableList.of(new ProcessorRef().setPipeline(serverImportForm.getPipelineId().toString())));
        List<ProcessorRef> generators = Lists.newArrayList();

        /**
         * Make a bunch of generators based on the type of file.
         */
        List<String> filePaths = Lists.newArrayList();
        ProcessorRef fileGen = pluginService.getProcessorRef("com.zorroa.core.generator.FileListGenerator");
        fileGen.setArg("paths", filePaths);

        for (String path: serverImportForm.getPaths()) {
            File file = new File(path);
            if (file.isDirectory()) {
                ProcessorRef vol = pluginService.getProcessorRef("com.zorroa.core.generator.FileSystemGenerator");
                vol.setArg("path", FileUtils.normalize(path));
                generators.add(vol);
            }
            else if (file.isFile()) {
                filePaths.add(FileUtils.normalize(path));
            }
        }

        if (!filePaths.isEmpty()) {
            generators.add(fileGen);
        }
        spec.setGenerators(generators);
        importService.create(spec);
        return "redirect:/admin/gui/imports";
    }

    @RequestMapping(value="/admin/gui/imports/search",  method= RequestMethod.POST)
    public String createImport(Model model,
                               @Valid @ModelAttribute("searchImportForm") SearchImportForm searchImportForm, BindingResult bindingResult) {

        standardModel(model);
        Pager paging = new Pager(1);
        model.addAttribute("page", paging);
        model.addAttribute("imports", importService.getAll(paging));
        model.addAttribute("pipelines", pipelineService.getAll(PipelineType.Import));

        if (bindingResult.hasErrors()) {
            model.addAttribute("search_errors", true);
            return "imports";
        }

        ImportSpec spec = new ImportSpec();
        spec.setName("search import by " + getUsername());
        spec.setProcessors(ImmutableList.of(new ProcessorRef().setPipeline(searchImportForm.getPipelineId().toString())));
        List<ProcessorRef> generators = Lists.newArrayList();

        String searchJson = searchImportForm.getSearch();
        AssetSearch search = Json.deserialize(searchJson, AssetSearch.class);

        ProcessorRef gen = pluginService.getProcessorRef("com.zorroa.core.generator.AssetSearchGenerator");
        gen.setArg("search", search);
        generators.add(gen);
        spec.setGenerators(generators);

        importService.create(spec);
        return "redirect:/admin/gui/imports";
    }

    private void standardModel(Model model) {

    }

}
