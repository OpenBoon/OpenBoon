package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.repository.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class UserController  {

    @Autowired
    UserDao userDao;

    @RequestMapping(value="/api/v1/login", method=RequestMethod.POST)
    public User login() {
        return userDao.get(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @RequestMapping(value="/api/v1/logout", method=RequestMethod.POST)
    public void logout(HttpServletRequest req) throws ServletException {
        req.logout();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/users")
    public List<User> getAll() {
        return userDao.getAll();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value="/api/v1/users/{id}")
    public User get(@PathVariable int id) {
        return userDao.get(id);
    }
}
