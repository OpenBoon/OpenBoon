package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.User;

import java.util.List;

/**
 * Created by chambers on 7/13/15.
 */
public interface UserService {

    User login();

    User get(String username);

    User get(int id);

    List<User> getAll();
}
