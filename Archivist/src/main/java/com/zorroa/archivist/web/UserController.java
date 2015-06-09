package com.zorroa.archivist.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.repository.UserDao;

@RestController
public class UserController  {

    @Autowired
    UserDao userDao;

    @RequestMapping(value="/login", method=RequestMethod.POST)
    public User login() {
        return userDao.get(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @RequestMapping(value="/users")
    public List<User> getAll() {
        return userDao.getAll();
    }


}
