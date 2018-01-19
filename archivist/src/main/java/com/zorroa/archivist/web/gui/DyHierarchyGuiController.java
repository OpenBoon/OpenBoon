package com.zorroa.archivist.web.gui;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.service.DyHierarchyService;
import com.zorroa.archivist.service.FolderService;
import com.zorroa.archivist.web.gui.forms.NewDyHiForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

/**
 * Created by chambers on 8/10/16.
 */
@Controller
public class DyHierarchyGuiController {

    private static final Logger logger = LoggerFactory.getLogger(DyHierarchyGuiController.class);

    @Autowired
    FolderService folderService;

    @Autowired
    DyHierarchyService dyHierarchyService;

    @RequestMapping(value="/admin/gui/dyhi", method=RequestMethod.POST)
    public String createDyhi(Model model,
                             @Valid @ModelAttribute("newDyHiForm") NewDyHiForm newDyHiForm,
                             BindingResult binding,
                             RedirectAttributes attr) {
        DyHierarchySpec spec = null;
        try {
            spec = convertFormToSpec(newDyHiForm);
        } catch (RuntimeException e) {
            binding.addError(new ObjectError("form", e.getMessage()));
        }

        /**
         * We're a child of a dyhi, so we can't add a dyhi.
         */
        Folder folder = folderService.get(newDyHiForm.getFolderId());
        if (folder.getDyhiId()!= null) {
            binding.addError(new ObjectError("form", "Cannot create dynamic hierarchies within dynamic hierarchies."));
        }

        if (binding.hasErrors()) {
            attr.addFlashAttribute("org.springframework.validation.BindingResult.newDyHiForm", binding);
            attr.addFlashAttribute("newDyHiForm", newDyHiForm);
            attr.addFlashAttribute("dyhiError", true);
            logger.warn("Failed to add DyHi: {}" + binding.getAllErrors());
            return "redirect:/admin/gui/folders/" + folder.getId();
        }
        else {
            /**
             * We're already a dyhi root.
             */
            if (folder.isDyhiRoot()) {
                DyHierarchy dyhi = dyHierarchyService.get(folder);
                if (spec.getLevels().isEmpty()) {
                    dyHierarchyService.delete(dyhi);
                    return "redirect:/admin/gui/folders/" + folder.getParentId();
                }
                else {
                    dyHierarchyService.update(dyhi.getId(), dyhi.setLevels(spec.getLevels()));
                }
            }
            else {
                /**
                 * We're not a dyhi root, its a freshy.
                 */

                dyHierarchyService.create(spec);
            }

            return "redirect:/admin/gui/folders/" + folder.getId();
        }
    }

    private DyHierarchySpec convertFormToSpec(NewDyHiForm form) {

        DyHierarchySpec spec = new DyHierarchySpec();
        spec.setFolderId(form.getFolderId());

        List<DyHierarchyLevel> levels = Lists.newArrayList();
        spec.setLevels(levels);

        if (form.getType() == null || form.getField() == null) {
            return spec;
        }

        for (int i=0; i< form.getType().size(); i++) {

            DyHierarchyLevel level = new DyHierarchyLevel();

            String type = form.getType().get(i);
            if (!JdbcUtils.isValid(type)) {
                throw new RuntimeException("The type cannot be null");
            }
            DyHierarchyLevelType lt = DyHierarchyLevelType.valueOf(type);
            if (lt == null) {
                throw new RuntimeException("Invalid level type: " + type);
            }

            String field = form.getField().get(i);
            if (!JdbcUtils.isValid(field)) {
                throw new RuntimeException("The field cannot be null or empty");
            }
            level.setType(lt);
            level.setField(field);
            levels.add(level);
        }

        return spec;
    }
}
