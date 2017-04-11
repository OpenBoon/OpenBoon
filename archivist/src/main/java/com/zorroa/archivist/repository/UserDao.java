package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.domain.Room;

import java.util.Collection;
import java.util.List;


public interface UserDao {

    User get(int id);

    User get(String username);

    User getByEmail(String email);

    User getByToken(String token);

    List<User> getAll();

    String getHmacKey(String username);

    boolean generateHmacKey(String username);

    List<User> getAll(Room room);

    boolean delete(User user);

    String getPassword(String username);

    boolean setSettings(User user, UserSettings settings);

    UserSettings getSettings(int id);

    boolean setPassword(User user, String password);

    boolean exists(String name);

    String setEnablePasswordRecovery(User user);

    boolean resetPassword(User user, String token, String password);

    boolean setEnabled(User user, boolean value);

    boolean update(User user, UserProfileUpdate update);

    PagedList<User> getAll(Pager paging);

    User create(UserSpec builder);

    User create(UserSpec builder, String source);

    List<User> getAllWithSession();

    long getCount();

    boolean hasPermission(User user, Permission permission);

    boolean hasPermission(User user, String type, String name);

    int setPermissions(User user, Collection<? extends Permission> perms);

    boolean addPermission(User user, Permission perm, boolean immutable);

    boolean removePermission(User user, Permission perm);

}
