package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserBuilder;
import com.zorroa.archivist.domain.UserUpdateBuilder;

import java.util.List;

public interface UserDao {

    User get(int id);
    User get(String username);

    boolean update(User user, UserUpdateBuilder builder);

    String getPassword(String username);
    List<User> getAll();

    User create(UserBuilder builder);
}
