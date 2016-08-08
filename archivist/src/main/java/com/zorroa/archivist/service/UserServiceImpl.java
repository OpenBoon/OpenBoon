package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.repository.SessionDao;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Access;
import com.zorroa.sdk.domain.Message;
import com.zorroa.sdk.domain.Room;
import com.zorroa.sdk.domain.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by chambers on 7/13/15.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    final static Set<String> PERMANENT_TYPES = ImmutableSet.of("user", "internal");

    @Autowired
    UserDao userDao;

    @Autowired
    SessionDao sessionDao;

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    FolderService folderService;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    MessagingService messagingService;

    @Override
    public User login() {
        return userDao.get(SecurityUtils.getUsername());
    }

    @Override
    public User create(UserSpec builder) {
        User user = userDao.create(builder);

        /*
         * Create a permission for this specific user.
         */
        Permission userPerm = permissionDao.create(
                new PermissionSpec("user", builder.getUsername()), true);

        /*
         * Get the permissions specified with the builder and add our
         * user permission to the list.
         */
        List<Permission> perms = permissionDao.getAll(builder.getPermissionIds());
        setPermissions(user, perms);

        /*
         * Add the user's permission as an immutable permission.
         */
        userDao.addPermission(user, userPerm, true);

        /*
         * Add the user's home folder
         */
        Folder userRoot = folderService.get(Folder.ROOT_ID, "Users");
        folderService.create(new FolderSpec()
                .setName(user.getUsername())
                .setParentId(userRoot.getId())
                .setAcl(new Acl().addEntry(userPerm, Access.Read, Access.Write)), false);

        transactionEventManager.afterCommitSync(() -> {
            messagingService.broadcast(new Message("USER_CREATE", user));
        });

        return user;
    }

    @Override
    public User get(String username) {
        return userDao.get(username);
    }

    @Override
    public User get(int id) {
        return userDao.get(id);
    }

    @Override
    public boolean exists(String username) {
        return userDao.exists(username);
    }

    @Override
    public List<User> getAll() {
        return userDao.getAll();
    }

    @Override
    public PagedList<User> getAll(Paging page) {
        return userDao.getAll(page);
    }

    @Override
    public long getCount() { return userDao.getCount(); }

    @Override
    public boolean setPassword(User user, String password) {
        return userDao.setPassword(user, password);
    }

    @Override
    public String getPassword(String username) {
        try {
            return userDao.getPassword(username);
        } catch (DataAccessException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Override
    public String getHmacKey(String username) {
        try {
            return userDao.getHmacKey(username);
        } catch (DataAccessException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Override
    public String generateHmacKey(String username) {
        if (userDao.generateHmacKey(username)) {
            return userDao.getHmacKey(username);
        }
        else {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Override
    public boolean update(User user, UserProfileUpdate form) {
        boolean result = userDao.update(user, form);
        if (result) {
            transactionEventManager.afterCommitSync(() -> {
                messagingService.broadcast(new Message("USER_UPDATE", get(user.getId())));
            });
        }
        return result;
    }

    @Override
    public boolean setEnabled(User user, boolean value) {
        boolean result =  userDao.setEnabled(user, value);

        if (result) {
            if (result) {
                Message msg = new Message(value ? "USER_ENABLED": "USER_DISABLED", user);
                transactionEventManager.afterCommitSync(() -> {
                    messagingService.broadcast(msg);
                });
            }
        }

        return result;
    }

    @Override
    public List<User> getAll(Room room) {
        return userDao.getAll(room);
    }

    @Override
    public Session getActiveSession() {
        return sessionDao.get(RequestContextHolder.currentRequestAttributes().getSessionId());
    }

    @Override
    public Session getSession(String cookieId) {
        return sessionDao.get(cookieId);
    }

    @Override
    public Session getSession(long id) {
        return sessionDao.get(id);
    }

    @Override
    public List<Permission> getPermissions() {
        return permissionDao.getAll();
    }

    @Override
    public PagedList<Permission> getPermissions(Paging page) {
        return permissionDao.getPaged(page);
    }

    @Override
    public PagedList<Permission> getPermissions(Paging page, PermissionFilter filter) {
        return permissionDao.getPaged(page, filter);
    }

    @Override
    public PagedList<Permission> getUserAssignablePermissions(Paging page) {
        return permissionDao.getPaged(page,
                new PermissionFilter()
                        .setAssignableToUser(true)
                        .setOrderBy("str_group, str_type"));
    }

    @Override
    public PagedList<Permission> getObjAssignablePermissions(Paging page) {
        return permissionDao.getPaged(page,
                new PermissionFilter()
                        .setAssignableToObj(true)
                        .setOrderBy("str_group, str_type"));
    }

    @Override
    public List<Permission> getPermissions(User user) {
        return permissionDao.getAll(user);
    }

    @Override
    public List<String> getPermissionNames() {
        return permissionDao.getAll().stream().map(p->p.getFullName()).collect(Collectors.toList());
    }

    @Override
    public Permission getPermission(String id) {
        if (id.contains(Permission.JOIN)) {
            return permissionDao.get(id);
        }
        else {
            return permissionDao.get(Integer.valueOf(id));
        }
    }

    @Override
    public void setPermissions(User user, Collection<Permission> perms) {
        /*
         * Don't let setPermissions set immutable permission types which can never
         * be added or removed via the external API.
         */
        List<Permission> filtered = perms.stream().filter(
                p->!PERMANENT_TYPES.contains(p.getType())).collect(Collectors.toList());
        userDao.setPermissions(user, filtered);
    }

    @Override
    public void addPermissions(User user, Collection<Permission> perms) {
        for (Permission p: perms) {
            if (PERMANENT_TYPES.contains(p.getType())) {
                continue;
            }
            userDao.addPermission(user, p, false);
        }
    }

    @Override
    public void removePermissions(User user, Collection<Permission> perms) {
        for (Permission p: perms) {
            // Don't allow removal of user permission.
            if (PERMANENT_TYPES.contains(p.getType())) {
                continue;
            }
            userDao.removePermission(user, p);
        }
    }

    @Override
    public Permission getPermission(int id) {
        return permissionDao.get(id);
    }

    @Override
    public Permission createPermission(PermissionSpec builder) {
        Permission perm = permissionDao.create(builder, false);
        transactionEventManager.afterCommitSync(() -> {
            messagingService.broadcast(new Message("PERMISSION_CREATE", builder));
        });
        return perm;
    }

    @Override
    public boolean hasPermission(User user, String type, String name) {
        return userDao.hasPermission(user, type, name);
    }

    @Override
    public boolean hasPermission(User user, Permission permission) {
        return userDao.hasPermission(user, permission);
    }

    @Override
    public boolean deletePermission(Permission permission) {
        boolean result = permissionDao.delete(permission);
        if (result) {
            transactionEventManager.afterCommitSync(() -> {
                messagingService.broadcast(new Message("PERMISSION_DELETE", permission));
            });
        }
        return result;
    }
}
