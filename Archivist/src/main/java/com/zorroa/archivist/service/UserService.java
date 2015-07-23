package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.Session;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserUpdateBuilder;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by chambers on 7/13/15.
 */
public interface UserService {

    User login();

    User get(String username);

    User get(int id);

    List<User> getAll();

    String getPassword(String username);

    boolean update(User user, UserUpdateBuilder builder);

    List<User> getAll(Room room);

    Session getSession(HttpSession session);

    Session getActiveSession();
}
