package com.zorroa.archivist.service;

import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.Session;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserUpdateBuilder;
import com.zorroa.archivist.repository.SessionDao;
import com.zorroa.archivist.repository.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by chambers on 7/13/15.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;

    @Autowired
    SessionDao sessionDao;

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

    @Override
    public List<User> getAll(Room room) {
        return userDao.getAll(room);
    }

    @Override
    public Session getSession(HttpSession session) {
        return sessionDao.get(session);
    }

    @Override
    public Session getActiveSession() {
        return sessionDao.get(RequestContextHolder.currentRequestAttributes().getSessionId());
    }
}
