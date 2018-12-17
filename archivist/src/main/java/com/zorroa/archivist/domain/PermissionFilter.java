package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.util.JdbcUtils;
import com.zorroa.archivist.repository.DaoFilter;

import java.util.Map;
import java.util.Set;

/**
 * Created by chambers on 8/8/16.
 */
public class PermissionFilter extends DaoFilter {

    private static final Map<String,String> sortMap = ImmutableMap.<String, String>builder()
            .put("id", "pk_permission")
            .put("name", "str_name")
            .put("type", "str_type")
            .put("description", "str_description")
            .build();

    public Boolean assignableToUser;
    public Boolean assignableToObj;
    public Set<String> types;
    public Set<String> names;

    public PermissionFilter() {  }

    public PermissionFilter(Map<String,String> sort) {
        this.setSort(sort);
    }

    public void build() {
        if (assignableToUser != null) {
            addToWhere("bool_assignable_to_user=?");
            addToValues(assignableToUser);
        }

        if (assignableToObj != null) {
            addToWhere("bool_assignable_to_obj=?");
            addToValues(assignableToObj);
        }

        if (JdbcUtils.isValid(types)) {
            addToWhere(JdbcUtils.in("str_type", types.size()));
            addToValues(types);
        }

        if (JdbcUtils.isValid(names)) {
            addToWhere(JdbcUtils.in("str_name", names.size()));
            addToValues(names);
        }
    }

    @Override
    public Map<String, String> getSortMap() {
        return sortMap;
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
