package com.zorroa.archivist.security;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.Access;
import com.zorroa.archivist.domain.Acl;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.User;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.PermissionSchema;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.index.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collection;
import java.util.Set;

public class SecurityUtils {

    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    public static String createPasswordHash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String getCookieId() {
        return RequestContextHolder.currentRequestAttributes().getSessionId();
    }

    public static String getUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new AuthenticationCredentialsNotFoundException("No login credentials specified");
        }
        else {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return user.getUsername();
        }
    }

    public static User getUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new AuthenticationCredentialsNotFoundException("No login credentials specified");
        }
        else {
            return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
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
        Set<Integer> perms = asset.getAttr("permissions."+ field, Json.SET_OF_INTS);
        return hasPermission(perms);
    }

    public static boolean hasPermission(String ... perms) {
        ImmutableSet<String> _perms = ImmutableSet.copyOf(perms);
        Collection<? extends GrantedAuthority> authorities =
                SecurityContextHolder.getContext().getAuthentication().getAuthorities();

        for (GrantedAuthority g: authorities) {
            if (_perms.contains(g.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPermission(Set<Integer> permIds) {
        if (permIds == null || permIds.isEmpty()) {
            return true;
        }
        return !Sets.intersection(permIds, SecurityUtils.getPermissionIds()).isEmpty();
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
        if (hasPermission("group::superuser")) {
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
        OrQueryBuilder result = QueryBuilders.orQuery();
        MissingQueryBuilder part1 = QueryBuilders.missingQuery("permissions.search");
        TermsQueryBuilder part2 = QueryBuilders.termsQuery("permissions.search",
                SecurityUtils.getPermissionIds());

        result.add(part1);
        result.add(part2);
        return result;
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

    public static void setReadPermissions(Source source, Collection<Permission> perms) {
        PermissionSchema ps = source.getAttr("permissions", PermissionSchema.class);
        if (ps == null) {
            ps = new PermissionSchema();
        }
        ps.getRead().clear();
        for (Permission p : perms) {
            ps.getRead().add(p.getId());
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

}
