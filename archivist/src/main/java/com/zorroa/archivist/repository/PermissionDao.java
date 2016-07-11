package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.User;
import com.zorroa.sdk.domain.Permission;
import com.zorroa.sdk.domain.PermissionBuilder;

import java.util.Collection;
import java.util.List;

/**
 * Created by chambers on 10/27/15.
 */
public interface PermissionDao {
    Permission create(PermissionBuilder builder, boolean immutable);

    Permission update(Permission permission);

    boolean updateUserPermission(String oldName, String newName);

    Permission get(int id);

    Permission get(String authority);

    List<Permission> getAll();

    List<Permission> getAll(User user);

    List<Permission> getAll(String type);

    Permission get(String type, String name);

    List<Permission> getAll(Integer[] ids);

    void setOnUser(User user, Collection<? extends Permission> perms);

    void setOnUser(User user, Permission... perms);

    boolean assign(User user, Permission perm, boolean immutable);

    boolean delete(Permission perm);

    boolean delete(User user);

    boolean hasPermission(User user, Permission permission);

    boolean hasPermission(User user, String type, String name);
}
