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

    String getHmacKey(String username);

    boolean generateHmacKey(String username);

    List<User> getAll(Room room);

    String getPassword(String username);

    boolean exists(String name);

    boolean setEnabled(User user, boolean value);

    boolean update(User user, UserUpdateBuilder builder);

    List<User> getAll(int size, int offset);

    User create(UserBuilder builder);

    List<User> getAllWithSession();

    int getCount();
}
