package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserUpdateBuilder;

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
}
