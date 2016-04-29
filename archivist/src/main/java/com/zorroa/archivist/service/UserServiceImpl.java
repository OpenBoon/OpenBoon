package com.zorroa.archivist.service;

import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.repository.SessionDao;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.sdk.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;

/**
 * Created by chambers on 7/13/15.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

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
    public User create(UserBuilder builder) {
        User user = userDao.create(builder);

        /*
         * Create a permission for this specific user.
         */
        Permission userPerm = permissionDao.create(
                new PermissionBuilder("user", builder.getUsername(), true));

        /*
         * Get the permissions specified with the builder and add our
         * user permission to the list.
         */
        List<Permission> perms = permissionDao.getAll(builder.getPermissionIds());
        permissionDao.setOnUser(user, perms);

        /*
         * Add the user's permission as an immutable permission.
         */
        permissionDao.assign(user, userPerm, true);

        /*
         * Add the user's home folder
         */
        Folder userRoot = folderService.get(Folder.ROOT_ID, "Users");
        folderService.create(new FolderBuilder()
                .setName(user.getUsername())
                .setParentId(userRoot.getId())
                .setAcl(new Acl().addEntry(userPerm, Access.Read, Access.Write)));

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
    public List<User> getAll() {
        return userDao.getAll();
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
    public boolean update(User user, UserUpdateBuilder builder) {
        // TODO: the permission for this user should be renamed or we
        // shouldn't allow people to change usernames.
        assert builder.getUsername() == null || user.getUsername() == builder.getUsername();

        if (builder.getPermissionIds() != null) {
            List<Permission> perms = permissionDao.getAll(builder.getPermissionIds());
            permissionDao.setOnUser(user, perms);
        }

        boolean result = userDao.update(user, builder);
        if (result) {
            transactionEventManager.afterCommitSync(() -> {
                messagingService.broadcast(new Message("USER_UPDATE", get(user.getId())));
            });
        }
        return result;
    }

    @Override
    public boolean delete(User user) {
        boolean result =  userDao.delete(user);
        /*
         * Delete the user's permission
         */
        try {
            permissionDao.delete(user);
        }
        catch (EmptyResultDataAccessException e) {
            logger.warn(String.format("The user %s has no user permission."));
        }

        /*
         * Delete the user's folder
         */
        try {
            Folder userFolder = folderService.get("/Users/" + user.getUsername());
            folderService.delete(userFolder);
        }
        catch (EmptyResultDataAccessException e) {
            logger.warn(String.format("The user %s has no user folder."));
        }

        if (result) {
            transactionEventManager.afterCommitSync(() -> {
                messagingService.broadcast(new Message("USER_DELETE", user));
            });
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
    public List<Permission> getPermissions(User user) {
        return permissionDao.getAll(user);
    }

    @Override
    public Permission getPermission(String name) {
        return permissionDao.get(name);
    }

    @Override
    public void setPermissions(User user, List<Permission> perms) {
        permissionDao.setOnUser(user, perms);
    }

    @Override
    public void setPermissions(User user, Permission ... perms) {
        permissionDao.setOnUser(user, perms);
    }

    @Override
    public Permission getPermission(int id) {
        return permissionDao.get(id);
    }

    @Override
    public Permission createPermission(PermissionBuilder builder) {
        Permission perm = permissionDao.create(builder);
        transactionEventManager.afterCommitSync(() -> {
            messagingService.broadcast(new Message("PERMISSION_CREATE", builder));
        });
        return perm;
    }

    @Override
    public boolean hasPermission(User user, Permission permission) {
        return permissionDao.hasPermission(user, permission);
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
