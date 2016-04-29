package com.zorroa.archivist.repository;

import com.zorroa.sdk.domain.Room;
import com.zorroa.sdk.domain.User;
import com.zorroa.sdk.domain.UserBuilder;
import com.zorroa.sdk.domain.UserUpdateBuilder;

import java.util.List;

public interface UserDao {

    User get(int id);
    User get(String username);

    List<User> getAll();
    List<User> getAll(Room room);

    String getPassword(String username);

    boolean delete(User user);

    boolean update(User user, UserUpdateBuilder builder);
    User create(UserBuilder builder);
}
