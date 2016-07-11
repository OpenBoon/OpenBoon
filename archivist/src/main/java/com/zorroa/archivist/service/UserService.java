package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.archivist.domain.UserUpdate;
import com.zorroa.sdk.domain.Permission;
import com.zorroa.sdk.domain.PermissionBuilder;
import com.zorroa.sdk.domain.Room;
import com.zorroa.sdk.domain.Session;

import java.util.List;

/**
 * Created by chambers on 7/13/15.
 */
public interface UserService {

    User login();

    User create(UserSpec builder);

    User get(String username);

    User get(int id);

    boolean exists(String username);

    List<User> getAll();

    List<User> getAll(int size, int offset);

    int getCount();

    String getPassword(String username);

    boolean setPassword(User user, String password);

    String getHmacKey(String username);

    String generateHmacKey(String username);

    boolean update(User user, UserUpdate builder);

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
