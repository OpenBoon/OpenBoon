package com.zorroa.archivist.web.gui;

import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.service.FolderService;
import com.zorroa.archivist.web.gui.forms.NewDyHiForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

/**
 * Created by chambers on 8/10/16.
 */
@Controller
public class FolderGuiController {

    private static final Logger logger = LoggerFactory.getLogger(FolderGuiController.class);

    @Autowired
    FolderService folderService;

    @RequestMapping("/gui/folders")
    public String folders(Model model) {
        return "redirect:/gui/folders/"+ Folder.ROOT_ID;
    }

    @RequestMapping("/gui/folders/{id}")
    public String folders(Model model, @PathVariable int id) {
        standardModel(model);
        folderModel(model, folderService.get(id));
        model.addAttribute("folderSpec", new FolderSpec());

        if (!model.containsAttribute("newDyHiForm")) {
            model.addAttribute("newDyHiForm", new NewDyHiForm());
        }
        return "folders";
    }

    @RequestMapping(value="/gui/folders/{id}", method= RequestMethod.POST)
    public String createFolder(Model model, @PathVariable int id,
                               @Valid @ModelAttribute("folderSpec") FolderSpec folderSpec,
                               BindingResult bindingResult) {
        standardModel(model);
        folderModel(model, folderService.get(id));

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            return "folders";
        }
        else {
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

    /**
     * Supports the data necessary for the overall template. (layout.html)
     * @param model
     */
    private void standardModel(Model model) {
        //model.addAttribute("stdJobs", ingestService.getAllIngests(IngestState.Running, 10));
    }
}
