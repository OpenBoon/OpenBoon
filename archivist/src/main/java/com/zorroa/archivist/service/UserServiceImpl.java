package com.zorroa.archivist.service;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.repository.SessionDao;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.archivist.repository.UserPresetDao;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.sdk.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStreamReader;
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
    JavaMailSender mailSender;

    @Autowired
    NetworkEnvironment networkEnv;

    @Autowired
    UserDao userDao;

    @Autowired
    SessionDao sessionDao;

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    UserPresetDao userPresetDao;

    @Autowired
    FolderService folderService;

    @Autowired
    TransactionEventManager txem;

    @Autowired
    LogService logService;

    private static final String SOURCE_LOCAL = "local";

    @Override
    public User create(UserSpec builder) {
        return create(builder, SOURCE_LOCAL);
    }

    @Override
    public User create(UserSpec builder, String source) {
        Permission userPerm = permissionDao.create(
                new PermissionSpec("user", builder.getUsername()), true);
        Folder userFolder = folderService.createUserFolder(
                builder.getUsername(), userPerm);

        builder.setHomeFolderId(userFolder.getId());
        builder.setUserPermissionId(userPerm.getId());

        User user = userDao.create(builder, source);

        /*
         * Grab the preset, if any.
         */
        UserPreset preset = null;
        if (builder.getUserPresetId() != null) {
            preset = userPresetDao.get(builder.getUserPresetId());
            userDao.setSettings(user, preset.getSettings());
        }

        /*
         * Get the permissions specified with the builder and add our
         * user permission to the list.q
         */
        Set<Permission> perms = Sets.newHashSet(permissionDao.getAll(builder.getPermissionIds()));
        if (preset!=null && preset.getPermissionIds() != null) {
            perms.addAll(permissionDao.getAll(preset.getPermissionIds().toArray(new Integer[]{})));
        }

        if (!perms.isEmpty()) {
            setPermissions(user, perms);
        }

        userDao.addPermission(user, userPerm, true);
        userDao.addPermission(user, permissionDao.get("group", "everyone"), true);

        txem.afterCommit(() -> {
            logService.logAsync(LogSpec.build(LogAction.Create, user));
        }, true);

        return userDao.get(user.getId());
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
    public User getByEmail(String email) {
        return userDao.getByEmail(email);
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
    public PagedList<User> getAll(Pager page) {
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
            txem.afterCommitSync(() -> {
                logService.logAsync(LogSpec.build(LogAction.Update, user));
            });
        }
        return result;
    }

    @Override
    public boolean delete(User user) {
        boolean result = userDao.delete(user);
        if (result) {
            try {
                permissionDao.delete(permissionDao.get(user.getPermissionId()));
            }
            catch (Exception e) {
                logger.warn("Failed to delete user permission for {}", user);
            }

            try {
                folderService.delete(folderService.get(user.getHomeFolderId()));
            }
            catch (Exception e) {
                logger.warn("Failed to delete home folder for {}", user);
            }

            txem.afterCommitSync(() -> {
                logService.logAsync(LogSpec.build(LogAction.Update, user));
            });
        }
        return result;
    }

    @Override
    public boolean updateSettings(User user, UserSettings settings) {
        boolean result = userDao.setSettings(user, settings);
        if (result) {
            txem.afterCommitSync(() -> {
                logService.logAsync(LogSpec.build(LogAction.Update, user));
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
                txem.afterCommitSync(() -> {
                    logService.logAsync(LogSpec.build(value ? "enable" : "disable", user));
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
    public PagedList<Permission> getPermissions(Pager page) {
        return permissionDao.getPaged(page);
    }

    @Override
    public PagedList<Permission> getPermissions(Pager page, PermissionFilter filter) {
        return permissionDao.getPaged(page, filter);
    }

    @Override
    public PagedList<Permission> getUserAssignablePermissions(Pager page) {
        return permissionDao.getPaged(page,
                new PermissionFilter()
                        .setAssignableToUser(true)
                        .setOrderBy("str_group, str_type"));
    }

    @Override
    public PagedList<Permission> getObjAssignablePermissions(Pager page) {
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
        txem.afterCommitSync(() -> {
            logService.logAsync(LogSpec.build("set_permission", user)
                    .putToAttrs("perms", perms.stream().map(ps->ps.getName()).collect(Collectors.toList())));
        });
    }

    @Override
    public void addPermissions(User user, Collection<Permission> perms) {
        for (Permission p: perms) {
            if (PERMANENT_TYPES.contains(p.getType())) {
                continue;
            }
            userDao.addPermission(user, p, false);
        }
        txem.afterCommitSync(() -> {
            logService.logAsync(LogSpec.build("add_permission", user)
                    .putToAttrs("perms", perms.stream().map(ps->ps.getName()).collect(Collectors.toList())));
        });
    }

    @Override
    public void removePermissions(User user, Collection<Permission> perms) {
        /**
         * Check to see if the permissions we are
         */
        for (Permission p: perms) {
            // Don't allow removal of user permission.
            if (PERMANENT_TYPES.contains(p.getType())) {
                continue;
            }
            userDao.removePermission(user, p);
        }
        txem.afterCommitSync(() -> {
            logService.logAsync(LogSpec.build("remove_permission", user)
                    .putToAttrs("perms", perms.stream().map(ps->ps.getName()).collect(Collectors.toList())));
        });
    }

    @Override
    public Permission getPermission(int id) {
        return permissionDao.get(id);
    }

    @Override
    public Permission createPermission(PermissionSpec builder) {
        Permission perm = permissionDao.create(builder, false);
        txem.afterCommitSync(() -> {
            logService.logAsync(LogSpec.build(LogAction.Create, perm));
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
            txem.afterCommitSync(() -> {
                logService.logAsync(LogSpec.build(LogAction.Delete, permission));
            });
        }
        return result;
    }

    @Override
    public List<UserPreset> getUserPresets() {
        return userPresetDao.getAll();
    }

    @Override
    public UserPreset getUserPreset(int id) {
        return userPresetDao.get(id);
    }

    @Override
    public boolean updateUserPreset(int id, UserPreset preset) {
        return userPresetDao.update(id, preset);
    }

    @Override
    public UserPreset createUserPreset(UserPresetSpec preset) {
        return userPresetDao.create(preset);
    }

    @Override
    public boolean deleteUserPreset(UserPreset preset) {
        return userPresetDao.delete(preset.getPresetId());
    }

    @Override
    public PasswordResetToken sendPasswordResetEmail(User user) {
        PasswordResetToken token = new PasswordResetToken(userDao.setEnablePasswordRecovery(user));
        if (token != null) {
            String url = networkEnv.getUri().toString() + "/password?token=" + token.toString();

            StringBuilder text = new StringBuilder(1024);
            text.append("Hello ");
            text.append(user.getFirstName());
            text.append(",\n\nClick on the link below to change your Zorroa login credentials.");
            text.append("\n\n" + url);
            text.append("\n\nIf you are not trying to change your Zorroa login credentials, please ignore this email.");

            String htmlMsg = null;
            try {
                htmlMsg = getTextResourceFile("emails/PasswordReset.html");
                htmlMsg = htmlMsg.replace("*|RESET_PASSWORD_URL|*", url + "&source=file_server");
                htmlMsg = htmlMsg.replace("*|FIRST_NAME|*", user.getUsername());
            } catch (IOException e) {
                logger.warn("Failed to open HTML template for onboarding. Sending text only.", e);
            }
            try {
                sendHTMLEmail(user, "Zorroa Account Verification", text.toString(), htmlMsg);
                token.setEmailSent(true);
            } catch (MessagingException e) {
                logger.warn("Email for sendPasswordResetEmail not sent, unexpected ", e);
            }
        }
        return token;
    }

    @Override
    public PasswordResetToken sendOnboardEmail(User user) {
        PasswordResetToken token = new PasswordResetToken(userDao.setEnablePasswordRecovery(user));

        if (token != null) {
            String url = networkEnv.getUri().toString() + "/onboard?token=" + token.toString();

            StringBuilder text = new StringBuilder(1024);
            text.append("Hello ");
            text.append(user.getFirstName());
            text.append(",\n\nWelcome to Zorroa. Let's get your stuff!");
            text.append(",\n\nClick on the link below to import your assets.");
            text.append("\n\n" + url);

            String htmlMsg = null;
            try {
                htmlMsg = getTextResourceFile("emails/Onboarding.html");
                htmlMsg = htmlMsg.replace("*|FIRST_NAME|*", user.getUsername());
                htmlMsg = htmlMsg.replace("*|FILE_SERVER_URL|*", url + "&source=file_server");
                htmlMsg = htmlMsg.replace("*|MY_COMPUTER_URL|*", url + "&source=my_computer");
                htmlMsg = htmlMsg.replace("*|CLOUD_SOURCE_URL|*", url + "&source=cloud");
            } catch (IOException e) {
                logger.warn("Failed to open HTML template for onboarding, Sending text only.", e);
            }

            try {
                sendHTMLEmail(user, "Welcome to Zorroa", text.toString(), htmlMsg);
                token.setEmailSent(true);
            } catch (MessagingException e) {
                logger.warn("Email for sendOnboardEmail not sent, unexpected ", e);
            }
        }
        return token;
    }

    private void sendHTMLEmail(User user, String subject, String text, String htmlMsg) throws MessagingException {
        String email = user.getEmail();
        if (ArchivistConfiguration.unittest) {
            email = System.getProperty("user.name") + "@zorroa.com";
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
        helper.setFrom("noreply@zorroa.com");
        helper.setTo(email);
        helper.setSubject(subject);
        if (htmlMsg != null) {
            helper.setText(text, htmlMsg);
        }
        else {
            helper.setText(text);
        }
        mailSender.send(mimeMessage);

    }

    private String getTextResourceFile(String fileName) throws IOException {
        return CharStreams.toString(new InputStreamReader(
                new ClassPathResource(fileName).getInputStream(), Charsets.UTF_8));
    }

    @Override
    public User resetPassword(String token, String password) {
        User user = userDao.getByToken(token);
        if (userDao.resetPassword(user, token, password)) {
            return user;
        }
        return null;
    }
}
