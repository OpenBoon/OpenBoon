package com.zorroa.archivist.security;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.PermissionSchema;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.Sets.intersection;

public class SecurityUtils {

    public static final String GROUP_ADMIN = "group::administrator";

    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    public static String createPasswordHash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String getUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new AuthenticationCredentialsNotFoundException("No login credentials specified");
        } else {
            try {
                User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                return user.getUsername();
            } catch (ClassCastException e) {
                throw new AuthenticationCredentialsNotFoundException("Invalid login creds for: " +
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            }
        }
    }

    public static UserAuthed getUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new AuthenticationCredentialsNotFoundException("No login credentials specified");
        } else {
            try {
                return ((UserAuthed) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            } catch (ClassCastException e) {
                throw new AuthenticationCredentialsNotFoundException("Invalid login creds for: " +
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            }
        }
    }

    public static UserAuthed getUserOrNull() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        } else {
            try {
                return ((UserAuthed) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            } catch (ClassCastException e) {
                throw new AuthenticationCredentialsNotFoundException("Invalid login creds for: " +
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            }
        }
    }

    /**
     * Return true if the user has permission to a particular type of permission.
     *
     * @param field
     * @param asset
     * @return
     */
    public static boolean hasPermission(String field, Asset asset) {
        Set<Integer> perms = asset.getAttr("permissions." + field, Json.SET_OF_INTS);
        return hasPermission(perms);
    }

    /**
     * Return true if the current user can export an asset.
     *
     * @param asset
     * @return
     */
    public static boolean canExport(Asset asset) {
        if (hasPermission("group::export")) {
            return true;
        }

        Set<Integer> perms = asset.getAttr("permissions.export", Json.SET_OF_INTS);
        return hasPermission(perms);
    }

    public static boolean hasPermission(Collection<String> perms) {
        return hasPermission(perms.toArray(new String[]{}));
    }

    public static boolean hasPermission(String ... perms) {
        ImmutableSet<String> _perms = ImmutableSet.copyOf(perms);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            if (authorities != null) {
                for (GrantedAuthority g : authorities) {
                    if (_perms.contains(g.getAuthority())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasPermission(Set<Integer> permIds) {
        if (permIds == null) {
            return true;
        }
        if (permIds == null || permIds.isEmpty()) {
            return true;
        }
        if (hasPermission(GROUP_ADMIN)) {
            return true;
        }
        return !intersection(permIds, SecurityUtils.getPermissionIds()).isEmpty();
    }

    /**
     * Test that the current logged in user has the given access
     * with a particular access control list.  Users with group::superuser
     * will always have access.
     *
     * @param acl
     * @param access
     * @return
     */
    public static boolean hasPermission(Acl acl, Access access) {
        if (acl == null) {
            return true;
        }
        if (hasPermission(GROUP_ADMIN)) {
            return true;
        }
        return acl.hasAccess(getPermissionIds(), access);
    }

    public static Set<Integer> getPermissionIds() {
        Set<Integer> result = Sets.newHashSet();
        for (GrantedAuthority g: SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
            Permission p = (Permission) g;
            result.add(p.getId());
        }
        return result;
    }

    public static QueryBuilder getPermissionsFilter() {
        if (hasPermission(GROUP_ADMIN)) {
            return null;
        }
        return QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery("permissions.read",
                SecurityUtils.getPermissionIds()));
    }

    public static void setWritePermissions(Source source, Collection<Permission> perms) {
        PermissionSchema ps = source.getAttr("permissions", PermissionSchema.class);
        if (ps == null) {
            ps = new PermissionSchema();
        }
        ps.getWrite().clear();
        for (Permission p : perms) {
            ps.getWrite().add(p.getId());
        }
        source.setAttr("permissions", ps);
    }

    public static void setExportPermissions(Source source, Collection<Permission> perms) {
        PermissionSchema ps = source.getAttr("permissions", PermissionSchema.class);
        if (ps == null) {
            ps = new PermissionSchema();
        }
        ps.getExport().clear();
        for (Permission p : perms) {
            ps.getExport().add(p.getId());
        }
        source.setAttr("permissions", ps);
    }

    /**
     * Return true if the user can set the new ACL.
     *
     * This function checks to ensure that A user isn't taking away access they have by accident.
     *
     * @param newAcl
     * @param oldAcl
     */
    public static void canSetAclOnFolder(Acl newAcl, Acl oldAcl, boolean created) {
        if (hasPermission(GROUP_ADMIN)) {
            return;
        }

        if (created) {
            if(!SecurityUtils.hasPermission(newAcl, Access.Read)) {
                throw new ArchivistWriteException("You cannot create a folder without read access to it.");
            }
        }
        else {
            /**
             * Here we check to to see if you have read/write/export access already
             * and we don't let you take away access from yourself.
             */
            boolean hasRead = SecurityUtils.hasPermission(oldAcl, Access.Read);
            boolean hasWrite = SecurityUtils.hasPermission(oldAcl, Access.Write);
            boolean hasExport = SecurityUtils.hasPermission(oldAcl, Access.Export);

            if (hasRead && !SecurityUtils.hasPermission(newAcl, Access.Read)) {
                throw new ArchivistWriteException("You cannot remove read access from yourself.");
            }

            if (hasWrite && !SecurityUtils.hasPermission(newAcl, Access.Write)) {
                throw new ArchivistWriteException("You cannot remove write access from yourself.");
            }

            if (hasExport && !SecurityUtils.hasPermission(newAcl, Access.Export)) {
                throw new ArchivistWriteException("You cannot remove export access from yourself.");
            }
        }
    }

}
