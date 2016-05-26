package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.*;

import java.util.List;

/**
 * Created by chambers on 7/13/15.
 */
public interface UserService {

    User login();

    User create(UserBuilder builder);

    User get(String username);

    User get(int id);

    List<User> getAll();

    String getPassword(String username);

    String getHmacKey(String username);

    boolean update(User user, UserUpdateBuilder builder);

    boolean disable(User user);

    List<User> getAll(Room room);

    Session getActiveSession();

    Session getSession(String cookieId);

    Session getSession(long id);

    List<Permission> getPermissions();

    List<Permission> getPermissions(User user);

    void setPermissions(User user, List<Permission> perms);

    void setPermissions(User user, Permission... perms);

    Permission getPermission(int id);

    Permission createPermission(PermissionBuilder builder);

    Permission getPermission(String name);

    boolean hasPermission(User user, String type, String name);

    boolean hasPermission(User user, Permission permission);

    boolean deletePermission(Permission permission);
}
