package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.domain.User;
import com.zorroa.sdk.domain.Permission;
import com.zorroa.sdk.domain.PermissionBuilder;

import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 10/27/15.
 */
public interface PermissionDao {

    /**
     * Types of permissions users cannot manipulate directly.
     */
    Set<String> PERMANENT_TYPES = ImmutableSet.of("user", "internal");

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

    boolean delete(Permission perm);

    boolean delete(User user);
}
