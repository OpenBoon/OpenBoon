package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.*;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpSession;
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

    boolean update(User user, UserUpdateBuilder builder);

    boolean delete(User user);

    List<User> getAll(Room room);

    Session getSession(HttpSession session);

    Session getActiveSession();

    List<GrantedAuthority> getGrantedAuthorities(User user);

    List<Permission> getPermissions();

    List<Permission> getPermissions(User user);

    void setPermissions(User user, List<Permission> perms);
}
