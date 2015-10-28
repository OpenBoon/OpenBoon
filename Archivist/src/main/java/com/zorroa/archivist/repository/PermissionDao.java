package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.PermissionBuilder;
import com.zorroa.archivist.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Created by chambers on 10/27/15.
 */
public interface PermissionDao {
    Permission create(PermissionBuilder builder);

    Permission update(Permission permission);

    Permission get(int id);

    Permission get(String name);

    List<Permission> getAll();

    List<Permission> getAll(User user);

    List<GrantedAuthority> getGrantedAuthorities(User user);

    void setPermissions(User user, List<Permission> perms);

    void setPermissions(User user, Permission... perms);
}
