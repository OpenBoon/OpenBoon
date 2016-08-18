package com.zorroa.archivist.domain;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.repository.DaoFilter;

import java.util.Set;

/**
 * Created by chambers on 8/8/16.
 */
public class PermissionFilter extends DaoFilter {

    public Boolean assignableToUser;
    public Boolean assignableToObj;
    public Set<String> types;
    public Set<String> names;

    public PermissionFilter() {
    }

    public void build() {
        if (assignableToUser != null) {
            where.add("bool_assignable_to_user=?");
            values.add(assignableToUser);
        }

        if (assignableToObj != null) {
            where.add("bool_assignable_to_obj=?");
            values.add(assignableToObj);
        }

        if (JdbcUtils.isValid(types)) {
            where.add(JdbcUtils.in("str_type", types.size()));
            values.addAll(types);
        }

        if (JdbcUtils.isValid(names)) {
            where.add(JdbcUtils.in("str_name", names.size()));
            values.addAll(names);
        }
    }

    public Boolean getAssignableToUser() {
        return assignableToUser;
    }

    public PermissionFilter setAssignableToUser(Boolean assignableToUser) {
        this.assignableToUser = assignableToUser;
        return this;
    }

    public Boolean getAssignableToObj() {
        return assignableToObj;
    }

    public PermissionFilter setAssignableToObj(Boolean assignableToObj) {
        this.assignableToObj = assignableToObj;
        return this;
    }

    public Set<String> getTypes() {
        return types;
    }

    public PermissionFilter setTypes(Set<String> types) {
        this.types = types;
        return this;
    }

    public Set<String> getNames() {
        return names;
    }

    public PermissionFilter setNames(Set<String> names) {
        this.names = names;
        return this;
    }
}
