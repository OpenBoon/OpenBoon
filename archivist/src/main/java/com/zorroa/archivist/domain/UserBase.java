package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.google.common.hash.HashCode;

/**
 * Created by chambers on 11/4/16.
 */
public class UserBase {

    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;

    public int getId() {
        return id;
    }

    public UserBase setId(int id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public UserBase setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public UserBase setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UserBase setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserBase setEmail(String email) {
        this.email = email;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (!(obj instanceof UserBase)) {
            return false;
        }
        UserBase other = (UserBase) obj;
        return other.getId() == getId();
    }

    @Override
    public int hashCode() {
        return HashCode.fromInt(getId()).hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", getId())
                .add("username", getUsername())
                .toString();
    }
}
