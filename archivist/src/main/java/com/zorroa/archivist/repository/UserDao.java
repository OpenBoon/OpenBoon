package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserProfileUpdate;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Room;

import java.util.Collection;
import java.util.List;


public interface UserDao {

    User get(int id);
    User get(String username);

    List<User> getAll();

    String getHmacKey(String username);

    boolean generateHmacKey(String username);

    List<User> getAll(Room room);

    String getPassword(String username);

    boolean setPassword(User user, String password);

    boolean exists(String name);

    boolean setEnabled(User user, boolean value);

    boolean update(User user, UserProfileUpdate update);

    PagedList<User> getAll(Paging paging);

    User create(UserSpec builder);

    List<User> getAllWithSession();

    long getCount();

    boolean hasPermission(User user, Permission permission);

    boolean hasPermission(User user, String type, String name);

    int setPermissions(User user, Collection<? extends Permission> perms);

    boolean addPermission(User user, Permission perm, boolean immutable);

    boolean removePermission(User user, Permission perm);
}
