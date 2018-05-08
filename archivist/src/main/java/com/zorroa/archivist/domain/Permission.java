package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by chambers on 10/27/15.
 */
public class Permission implements Loggable<UUID>, Serializable, GrantedAuthority {

    public static final String JOIN = "::";

    private UUID id;
    private String name;
    private String type;
    private String description;
    private boolean immutable;

    public String getFullName() {
        return type.concat(JOIN).concat(name);
    }

    public UUID getId() {
        return id;
    }

    public Permission setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public Permission setImmutable(boolean immutable) {
        this.immutable = immutable;
        return this;
    }

    @JsonIgnore
    public String getAuthority() {
        return type.concat(JOIN).concat(name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, 829);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id)
                .add("name", name)
                .add("type", type)
                .add("description", description)
                .toString();
    }

    @Override
    public UUID getTargetId() {
        return id;
    }
}
