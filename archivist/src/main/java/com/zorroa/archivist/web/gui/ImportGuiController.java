package com.zorroa.archivist.web.gui;

import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.ImportService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.archivist.service.PipelineService;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.plugins.ModuleRef;
import com.zorroa.sdk.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.File;
import java.util.List;

/**
 * Created by chambers on 7/28/16.
 */
@Controller
public class ImportGuiController {

    @Autowired
    PipelineService pipelineService;

    @Autowired
    JobService jobService;

    @Autowired
    ImportService importService;

    @Autowired
    Validator validator;

    @RequestMapping("/gui/imports/{id}")
    public String getImport(Model model, @PathVariable int id, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        Paging paging = new Paging(page);
        model.addAttribute("page", paging);
        model.addAttribute("imp", jobService.get(id));
        model.addAttribute("tasks", jobService.getAllTasks(id, paging));
        model.addAttribute("serverImportForm", new NewServerImportForm());
        model.addAttribute("pipelines", pipelineService.getAll());
        return "import";
    }

    @RequestMapping("/gui/imports")
    public String getAllImports(Model model, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        Paging paging = new Paging(page);
        model.addAttribute("page", paging);
        model.addAttribute("imports", importService.getAll(paging));
        model.addAttribute("pipelines", pipelineService.getAll());
        model.addAttribute("serverImportForm", new NewServerImportForm());
        return "imports";
    }

    @RequestMapping(value="/gui/imports/server",  method= RequestMethod.POST)
    public String createImport(Model model,
                               @Valid @ModelAttribute("serverImportForm") NewServerImportForm serverImportForm, BindingResult bindingResult) {

        standardModel(model);
        Paging paging = new Paging(1);
        model.addAttribute("page", paging);
        model.addAttribute("imports", importService.getAll(paging));
        model.addAttribute("pipelines", pipelineService.getAll());

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            return "imports";
        }

        ImportSpec spec = new ImportSpec();
        spec.setName("server import by " + SecurityUtils.getUsername());
        spec.setPipelineId(serverImportForm.getPipelineId());
        List<ModuleRef> generators = Lists.newArrayList();

        /**
         * Make a bunch of generators based on the type of file.
         */
        List<String> filePaths = Lists.newArrayList();
        ModuleRef fileGen = new ModuleRef("generator:zorroa-core:ListOfFiles");
        fileGen.setArg("paths", filePaths);

        for (String path: serverImportForm.getPaths()) {
            File file = new File(path);
            if (file.isDirectory()) {
                ModuleRef vol = new ModuleRef("generator:zorroa-core:SharedVolume");
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
        return "redirect:/gui/imports";
    }

    private void standardModel(Model model) {

    }

}
