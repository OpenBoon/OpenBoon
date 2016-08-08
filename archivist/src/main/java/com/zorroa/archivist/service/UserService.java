package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.*;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Room;
import com.zorroa.sdk.domain.Session;

import java.util.Collection;
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

    PagedList<User> getAll(Paging page);

    long getCount();

    String getPassword(String username);

    boolean setPassword(User user, String password);

    String getHmacKey(String username);

    String generateHmacKey(String username);

    boolean update(User user, UserProfileUpdate builder);

    boolean setEnabled(User user, boolean value);

    List<User> getAll(Room room);

    Session getActiveSession();

    Session getSession(String cookieId);

    Session getSession(long id);

    List<Permission> getPermissions();

    List<Permission> getPermissions(User user);

    void setPermissions(User user, Collection<Permission> perms);

    void addPermissions(User user, Collection<Permission> perms);

    void removePermissions(User user, Collection<Permission> perms);

    Permission getPermission(int id);

    Permission createPermission(PermissionSpec builder);

    List<String> getPermissionNames();

    Permission getPermission(String name);

    boolean hasPermission(User user, String type, String name);

    boolean hasPermission(User user, Permission permission);

    boolean deletePermission(Permission permission);
}
