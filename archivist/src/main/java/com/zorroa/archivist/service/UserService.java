package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.domain.Room;
import com.zorroa.sdk.domain.Session;

import java.util.Collection;
import java.util.List;

/**
 * Created by chambers on 7/13/15.
 */
public interface UserService {

    User create(UserSpec builder);

    User create(UserSpec builder, String source);

    User get(String username);

    User get(int id);

    boolean exists(String username);

    List<User> getAll();

    PagedList<User> getAll(Pager page);

    long getCount();

    String getPassword(String username);

    boolean setPassword(User user, String password);

    String getHmacKey(String username);

    String generateHmacKey(String username);

    boolean update(User user, UserProfileUpdate builder);

    boolean delete(User user);

    boolean updateSettings(User user, UserSettings settings);

    boolean setEnabled(User user, boolean value);

    List<User> getAll(Room room);

    Session getActiveSession();

    Session getSession(String cookieId);

    Session getSession(long id);

    List<Permission> getPermissions();

    PagedList<Permission> getPermissions(Pager page, PermissionFilter filter);

    PagedList<Permission> getPermissions(Pager page);

    PagedList<Permission> getUserAssignablePermissions(Pager page);

    PagedList<Permission> getObjAssignablePermissions(Pager page);

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

    // Presets
    List<UserPreset> getUserPresets();
    UserPreset getUserPreset(int id);
    boolean updateUserPreset(int id, UserPreset preset);
    UserPreset createUserPreset(UserPresetSpec preset);
    boolean deleteUserPreset(UserPreset preset);
}
