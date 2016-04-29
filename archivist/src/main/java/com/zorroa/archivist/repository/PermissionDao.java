package com.zorroa.archivist.repository;

import com.zorroa.sdk.domain.Permission;
import com.zorroa.sdk.domain.PermissionBuilder;
import com.zorroa.sdk.domain.User;

import java.util.Collection;
import java.util.List;

/**
 * Created by chambers on 10/27/15.
 */
public interface PermissionDao {
    Permission create(PermissionBuilder builder);

    Permission update(Permission permission);

    Permission get(int id);

    Permission get(String authority);

    List<Permission> getAll();

    List<Permission> getAll(User user);

    List<Permission> getAll(String type);

    List<Permission> getAll(Integer[] ids);

    void setOnUser(User user, Collection<? extends Permission> perms);

    void setOnUser(User user, Permission... perms);

    boolean assign(User user, Permission perm, boolean immutable);

    boolean delete(Permission perm);

    boolean delete(User user);

    boolean hasPermission(User user, Permission permission);
}
