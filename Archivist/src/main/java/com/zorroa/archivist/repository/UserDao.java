package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserBuilder;

import java.util.List;

public interface UserDao {

    User get(int id);
    User get(String username);
    String getPassword(String username);
    List<User> getAll();

    User create(UserBuilder builder);
}
