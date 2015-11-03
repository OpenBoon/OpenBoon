package com.zorroa.archivist.domain;

import com.zorroa.archivist.sdk.domain.Permission;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by chambers on 11/3/15.
 */
public class InternalPermission extends Permission implements GrantedAuthority {

    @Override
    public String getAuthority() {
        return getName();
    }

    public static List<InternalPermission> upcast(Collection<Permission> perms) {
        return perms.stream().map(p -> (InternalPermission) p).collect(Collectors.toList());
    }
}
