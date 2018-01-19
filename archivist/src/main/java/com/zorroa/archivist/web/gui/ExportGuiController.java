package com.zorroa.archivist.web.gui;

import com.zorroa.archivist.service.ExportService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.sdk.domain.Pager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by chambers on 8/18/16.
 */
@Controller
public class ExportGuiController {

    @Autowired
    ExportService exportService;

    @Autowired
    JobService jobService;

    @RequestMapping("/admin/gui/exports")
    public String getAllImports(Model model, @RequestParam(value="page", required=false) Integer page) {
        Pager paging = new Pager(page);
        model.addAttribute("page", paging);
        model.addAttribute("exports", exportService.getAll(paging));
        return "exports";
    }


    @RequestMapping("/admin/gui/exports/{id}")
    public String getImport(Model model, @PathVariable int id, @RequestParam(value="page", required=false) Integer page) {
        Pager paging = new Pager(page);
        model.addAttribute("page", paging);
        model.addAttribute("job", jobService.get(id));
        model.addAttribute("tasks", jobService.getAllTasks(id, paging));
        return "export";
    }


}
