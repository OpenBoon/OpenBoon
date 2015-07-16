package com.zorroa.archivist.service;

import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserUpdateBuilder;
import com.zorroa.archivist.repository.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by chambers on 7/13/15.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;

    @Override
    public User login() {
        return userDao.get(SecurityUtils.getUsername());
    }

    @Override
    public User get(String username) {
        return userDao.get(username);
    }

    @Override
    public User get(int id) {
        return userDao.get(id);
    }

    @Override
    public List<User> getAll() {
        return userDao.getAll();
    }

    @Override
    public String getPassword(String username) {
        try {
            return userDao.getPassword(username);
        } catch (DataAccessException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Override
    public boolean update(User user, UserUpdateBuilder builder) {
        return userDao.update(user, builder);
    }
}
