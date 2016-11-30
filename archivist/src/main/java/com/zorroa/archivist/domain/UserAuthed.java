package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableSet;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Created by chambers on 11/28/16.
 */
public class UserAuthed extends User implements UserDetails {

    private boolean created;

    private final Collection<? extends GrantedAuthority> authorities;

    public UserAuthed(User user) {
        this(user, ImmutableSet.of());
    }

    public UserAuthed(User user, Collection<? extends GrantedAuthority> authorities) {
        this.setFirstName(user.getFirstName());
        this.setLastName(user.getLastName());
        this.setSettings(user.getSettings());
        this.setEmail(user.getEmail());
        this.setEnabled(user.getEnabled());
        this.setId(user.getId());
        this.setUsername(user.getUsername());
        this.setSettings(user.getSettings());
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
       return authorities;
    }

    @Override
    public String getPassword() {
        return "HIDDEN";
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return getEnabled();
    }

    public boolean isCreated() {
        return created;
    }

    public UserAuthed setCreated(boolean created) {
        this.created = created;
        return this;
    }
}
