package com.zorroa.archivist.web.gui;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.service.FilterService;
import com.zorroa.archivist.service.UserService;
import com.zorroa.archivist.web.gui.forms.NewFilterForm;
import com.zorroa.sdk.domain.Pager;
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
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.List;

import static com.sun.corba.se.spi.activation.IIOP_CLEAR_TEXT.value;

/**
 * Created by chambers on 8/9/16.
 */
@Controller
public class FilterGuiController {

    private static final Logger logger = LoggerFactory.getLogger(FilterGuiController.class);

    @Autowired
    UserService userService;

    @Autowired
    FilterService filterService;

    @RequestMapping("/gui/filters")
    public String getFilters(Model model,
                             @RequestParam(value="page", required=false) Integer page,
                             @RequestParam(value="count", required=false) Integer count) {
        standardModel(model);
        Pager pager = new Pager(page, count);
        model.addAttribute("filters", filterService.getPaged(pager));
        model.addAttribute("page", pager);
        return "filters";
    }

    @RequestMapping("/gui/filters/new")
    public String createFilterForm(Model model) {
        standardModel(model);
        model.addAttribute("newFilterForm", new NewFilterForm());
        return "filters_new";
    }

    @RequestMapping(value="/gui/filters/new", method= RequestMethod.POST)
    public String createFilter(Model model,
                                     @Valid @ModelAttribute("newFilterForm") NewFilterForm newFilterForm,
                                     BindingResult bindingResult) {

        logger.info("form: {}", newFilterForm);
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            return "filters_new";
        }

        try {
            filterService.create(convertToFilterSpec(newFilterForm));
        } catch (Exception e) {
            logger.warn("Failed to convert filter form: {}", newFilterForm, e);
            model.addAttribute("errors", true);
            bindingResult.addError(new ObjectError("filter", e.getMessage()));
            return "filters_new";
        }

        standardModel(model);
        return "redirect:/gui/filters";
    }

    /**
     * Convert the NewFilterForm to a FilterSpec.
     *
     * @param form
     * @return
     */
    private FilterSpec convertToFilterSpec(NewFilterForm form) {

        FilterSpec spec = new FilterSpec();
        spec.setDescription(form.getDescription());

        List<MatcherSpec> matchers = Lists.newArrayList();
        spec.setMatchers(matchers);

        for (int i=0; i<form.getAttribute().size(); i++) {
            MatcherSpec mspec = new MatcherSpec();

            // ATTR
            String attr = form.getAttribute().get(i);
            if (!JdbcUtils.isValid(attr)) {
                continue;
            }
            mspec.setAttr(attr);

            // OP
            String op = form.getOp().get(i);
            if (!JdbcUtils.isValid(op)) {
                throw new RuntimeException("Invalid null matching operation");
            }
            mspec.setOp(form.getOp().get(i));

            // VAL
            String val = form.getValue().get(i);
            if (!JdbcUtils.isValid(val) && !op.equals("exists")) {
                throw new RuntimeException("Invalid attribute matching value.");
            }

            mspec.setValue(value);
            matchers.add(mspec);
        }

        logger.info("matchers: {}", matchers.size());

        if (matchers.isEmpty()) {
            throw new RuntimeException("You must provide at least 1 valid matcher.");
        }

        List<ActionSpec> actions = Lists.newArrayList();
        spec.setActions(actions);
        if (JdbcUtils.isValid(form.getPerm())) {
            for (int i=0; i<form.getPerm().size(); i++) {
                ActionSpec aspec = new ActionSpec();
                aspec.setType(Action.Type.SET_PERMISSION);

                // Check for a invalid permissions from an unset select box.
                String permName = form.getPerm().get(i);
                if (!permName.contains(Permission.JOIN)) {
                    continue;
                }

                try {
                    Permission perm = userService.getPermission(form.getPerm().get(i));
                    aspec.setPermissionId(perm.getId());
                } catch (Exception e) {
                    throw new RuntimeException("Invalid permission.");
                }

                int access = 0;
                if (JdbcUtils.isValid(form.getRead())) {
                    try {
                        if (form.getRead().get(i) != null) {
                            access += 1;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        //
                    }
                }
                if (JdbcUtils.isValid(form.getWrite())) {
                    try {
                        if (form.getWrite().get(i) != null) {
                            access += 2;
                        }
                    } catch (IndexOutOfBoundsException e) {

                    }
                }
                if (JdbcUtils.isValid(form.getExport())) {
                    try {
                        if (form.getExport().get(i) != null) {
                            access += 4;
                        }
                    } catch (IndexOutOfBoundsException e) {

                    }
                }

                aspec.setIntValue(access);
                actions.add(aspec);
            }
        }

        if (actions.isEmpty()) {
            throw new RuntimeException("You must provide at least 1 valid action.");
        }
        return spec;
    }

    private void standardModel(Model model) {

    }
}
