package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserBuilder;
import com.zorroa.archivist.domain.UserUpdateBuilder;
import com.zorroa.archivist.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class UserController  {

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
    public User update(@RequestBody UserUpdateBuilder builder, @PathVariable int id) {

        /*
        * TODO: check if user is the current user or has role admin to allow
        * users to change their own information.
        *
        */
        User user = userService.get(id);
        userService.update(user, builder);
        return userService.get(id);
    }
}
