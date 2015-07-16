package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.hash.HashCode;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Set<String> roles;
    private Boolean enabled;

    public User() { }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof User)) {
            return false;
        }
        User other = (User) obj;
        return other.getId() == getId();
    }

    public int hashCode() {
        return HashCode.fromInt(getId()).hashCode();
    }

}
