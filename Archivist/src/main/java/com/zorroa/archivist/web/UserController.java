package com.zorroa.archivist.web;

import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.Session;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserBuilder;
import com.zorroa.archivist.domain.UserUpdateBuilder;
import com.zorroa.archivist.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
public class UserController  {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @RequestMapping(value="/api/v1/login", method=RequestMethod.POST)
    public User login() {
        return userService.login();
    }

    @RequestMapping(value="/api/v1/logout", method=RequestMethod.POST)
    public void logout(HttpServletRequest req) throws ServletException {
        req.logout();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/users")
    public List<User> getAll() {
        return userService.getAll();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/users", method=RequestMethod.POST)
    public User create(@RequestBody UserBuilder builder) {
        return userService.create(builder);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/users/{id}")
    public User get(@PathVariable int id) {
        return userService.get(id);
    }

    @RequestMapping(value="/api/v1/users/{id}", method=RequestMethod.PUT)
    public User update(@RequestBody UserUpdateBuilder builder, @PathVariable int id, HttpSession httpSession) {
        Session session = userService.getSession(httpSession);

        if (session.getUserId() == id || SecurityUtils.hasPermission("ROLE_ADMIN")) {
            User user = userService.get(id);
            userService.update(user, builder);
            return userService.get(id);
        }
        else {
            throw new SecurityException("You do not have the access to modify this user.");
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/users/{id}", method=RequestMethod.DELETE)
    public void delete(@PathVariable int id) {
        User user = userService.get(id);
        userService.delete(user);
    }
}
