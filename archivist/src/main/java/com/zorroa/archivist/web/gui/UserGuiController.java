package com.zorroa.archivist.web.gui;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserProfileUpdate;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.archivist.service.SearchService;
import com.zorroa.archivist.service.UserService;
import com.zorroa.archivist.web.gui.forms.UserPasswordForm;
import com.zorroa.archivist.web.gui.forms.UserStateForm;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.search.AssetSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Created by chambers on 7/31/16.
 */
@Controller
public class UserGuiController {

    private static final Logger logger = LoggerFactory.getLogger(UserGuiController.class);

    @Autowired
    UserService userService;

    @Autowired
    SearchService searchService;

    @RequestMapping("/gui/users")
    public String users(Model model, @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);
        Paging paging = new Paging(page);
        model.addAttribute("page", paging);
        model.addAttribute("allUsers", userService.getAll(paging));
        return "users";
    }

    @RequestMapping("/gui/users/{id}")
    public String userProfile(Model model, @PathVariable int id) {
        standardModel(model);
        model.addAttribute("user", userService.get(id));
        return "user";
    }

    @RequestMapping("/gui/users/{id}/account")
    public String userAccount(Model model, @PathVariable int id) {
        standardModel(model);
        model.addAttribute("user", userService.get(id));
        return "user_account";
    }

    @RequestMapping("/gui/users/{id}/permissions")
    public String userPermissions(Model model, @PathVariable int id) {
        User user = userService.get(id);
        standardModel(model);
        model.addAttribute("user", user);
        model.addAttribute("permissions", userService.getPermissions(user));
        model.addAttribute("assetCount", searchService.count(new AssetSearch()));
        model.addAttribute("allPermissions", userService.getPermissions());
        return "user_permissions";
    }

    @RequestMapping(value="/gui/users/{id}/account", method=RequestMethod.POST)
    public String updateUserPassword(Model model, @PathVariable int id,
                                     @Valid @ModelAttribute("userPasswordForm") UserPasswordForm form,
                                     BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.get(id));
            model.addAttribute("errors", true);
            return "user_account";
        }

        User user = userService.get(id);
        userService.setPassword(user, form.getPassword());
        standardModel(model);
        model.addAttribute("user", user);
        return "redirect:/gui/users/" + user.getId() + "/account";
    }

    @RequestMapping(value="/gui/users/{id}/state", method=RequestMethod.POST)
    public String updateUserState(Model model, @PathVariable int id, @ModelAttribute("userStateForm") UserStateForm userStateForm) {
        standardModel(model);

        User user = userService.get(id);
        userService.setEnabled(user, userStateForm.isEnabled());
        model.addAttribute("user", user);
        return "redirect:/gui/users/" + user.getId() + "/account";
    }

    @RequestMapping(value="/gui/users/{id}", method= RequestMethod.POST)
    public String updateUserProfile(Model model, @PathVariable int id,
                                    @Valid @ModelAttribute("userProfileForm") UserProfileUpdate form,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.get(id));
            model.addAttribute("errors", true);
            return "user";
        }

        standardModel(model);
        userService.update(userService.get(id), form);
        model.addAttribute("success", true);
        model.addAttribute("user", userService.get(id));
        return "redirect:/gui/user/" + id;
    }

    @RequestMapping(value="/gui/users", method=RequestMethod.POST)
    public String createUser(Model model,
                             @Valid @ModelAttribute("newUserForm") UserSpec newUserForm,
                             BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            model.addAttribute("page", new Paging(1));
            model.addAttribute("allUsers", userService.getAll());
            return "users";
        }

        standardModel(model);
        model.addAttribute("allUsers", userService.getAll());
        User user = userService.create(newUserForm);
        return "redirect:/gui/users/"+ user.getId();
    }

    /**
     * Supports the data necessary for the overall template. (layout.html)
     * @param model
     */
    private void standardModel(Model model) {
        model.addAttribute("userPasswordForm", new UserPasswordForm());
        model.addAttribute("userProfileForm", new UserProfileUpdate());
        model.addAttribute("userStateForm", new UserStateForm());
        model.addAttribute("newUserForm", new UserSpec());
        //model.addAttribute("stdJobs", ingestService.getAllIngests(IngestState.Running, 10));
    }
}
