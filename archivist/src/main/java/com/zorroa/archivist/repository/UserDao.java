package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.archivist.domain.UserUpdate;
import com.zorroa.sdk.domain.Room;

import java.util.List;


public interface UserDao {

    User get(int id);
    User get(String username);

    List<User> getAll();

    String getHmacKey(String username);

    boolean generateHmacKey(String username);

    List<User> getAll(Room room);

    String getPassword(String username);

    boolean setPassword(User user, String password);

    boolean exists(String name);

    boolean setEnabled(User user, boolean value);

    boolean update(User user, UserUpdate update);

    List<User> getAll(int size, int offset);

    User create(UserSpec builder);

    List<User> getAllWithSession();

    int getCount();
}
