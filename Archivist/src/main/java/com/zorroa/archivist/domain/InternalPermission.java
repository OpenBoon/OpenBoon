package com.zorroa.archivist.domain;

import com.zorroa.archivist.sdk.domain.Permission;
import org.springframework.security.core.GrantedAuthority;

/**
 * Created by chambers on 11/3/15.
 */
public class InternalPermission extends Permission implements GrantedAuthority {

    @Override
    public String getAuthority() {
        return getName();
    }

}
