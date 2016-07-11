package com.zorroa.archivist.web.api;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.archivist.domain.UserUpdate;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.UserService;
import com.zorroa.sdk.domain.Permission;
import com.zorroa.sdk.domain.Session;
import com.zorroa.sdk.exception.ArchivistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserController  {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @Autowired
    Validator validator;

    @RequestMapping(value="/api/v1/generate_api_key", method=RequestMethod.POST)
    public String generate_api_key() {
        return userService.generateHmacKey(SecurityUtils.getUsername());
    }

    @RequestMapping(value="/api/v1/login", method=RequestMethod.POST)
    public User login() {
        return userService.login();
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/users")
    public List<User> getAll() {
        return userService.getAll();
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/users", method=RequestMethod.POST)
    public User create(@Valid @RequestBody UserSpec builder, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new RuntimeException("Failed to add user");
        }
        return userService.create(builder);
    }

    @RequestMapping(value="/api/v1/users/{id}")
    public User get(@PathVariable int id) {
        return userService.get(id);
    }

    @RequestMapping(value="/api/v1/users/{username}/_exists")
    public Map get(@PathVariable String username) {
        return ImmutableMap.of("result", userService.exists(username));
    }

    @RequestMapping(value="/api/v1/users/{id}", method=RequestMethod.PUT)
    public User update(@RequestBody UserUpdate builder, @PathVariable int id) {
        Session session = userService.getActiveSession();

        if (session.getUserId() == id || SecurityUtils.hasPermission("group::manager", "group::systems")) {
            User user = userService.get(id);
            userService.update(user, builder);
            return userService.get(id);
        }
        else {
            throw new SecurityException("You do not have the access to modify this user.");
        }
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/users/{id}", method=RequestMethod.DELETE)
    public void disable(@PathVariable int id) {
        User user = userService.get(id);
        logger.info("active session: {}", userService.getActiveSession());
        logger.info("user {}", user);
        if (user.getId() == userService.getActiveSession().getUserId()) {
            throw new ArchivistException("You cannot disable your own user.");
        }
        userService.disable(user);
    }

    /**
     * Return the list of permissions for the given user id.
     *
     * @param id
     * @return
     */
    @RequestMapping(value="/api/v1/users/{id}/permissions", method=RequestMethod.GET)
    public List<Permission> getPermissions(@PathVariable int id) {
        User user = userService.get(id);
        return userService.getPermissions(user);
    }

    /**
     * Set an array of integers that correspond to permission IDs.  These
     * will be assigned to the user as permissions.  The Permission object
     * assigned are returned back.
     *
     * @param pids
     * @param id
     * @return
     */
    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::superuser')")
    @RequestMapping(value="/api/v1/users/{id}/permissions", method=RequestMethod.PUT)
    public List<Permission> setPermissions(@RequestBody List<Integer> pids, @PathVariable int id) {
        User user = userService.get(id);
        List<Permission> perms = pids.stream().map(
                i->userService.getPermission(i)).collect(Collectors.<Permission>toList());

        userService.setPermissions(user, perms);
        return userService.getPermissions(user);
    }
}
