package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.PermissionSpec;
import com.zorroa.archivist.domain.User;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;

/**
 * Created by chambers on 10/27/15.
 */
public interface PermissionDao {

    Permission create(PermissionSpec builder, boolean immutable);

    Permission update(Permission permission);

    boolean updateUserPermission(String oldName, String newName);

    Permission get(int id);

    Permission get(String authority);

    List<Permission> getAll();

    PagedList<Permission> getPaged(Pager page, DaoFilter filter);

    PagedList<Permission> getPaged(Pager page);

    long count();

    long count(DaoFilter filter);

    List<Permission> getAll(User user);

    List<Permission> getAll(String type);

    Permission get(String type, String name);

    List<Permission> getAll(Integer[] ids);

    boolean delete(Permission perm);
}
