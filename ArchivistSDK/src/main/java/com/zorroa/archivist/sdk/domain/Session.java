package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;
import com.google.common.hash.HashCode;

/**
 * Created by chambers on 7/16/15.
 */
public class Session {

    private long id;
    private int userId;
    private String username;
    private String cookieId;
    private long refreshTime;
    private SessionAttrs attrs;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getRefreshTime() {
        return refreshTime;
    }

    public void setRefreshTime(long refreshTime) {
        this.refreshTime = refreshTime;
    }

    public String getCookieId() {
        return cookieId;
    }

    public void setCookieId(String cookieId) {
        this.cookieId = cookieId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public SessionAttrs getAttrs() {
        return attrs;
    }

    public Session setAttrs(SessionAttrs attrs) {
        this.attrs = attrs;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Session.class)
                .add("id", id)
                .add("user", username)
                .add("cookie", cookieId)
                .add("attrs", attrs)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Session)) {
            return false;
        }
        Session other = (Session) obj;
        return other.getId() == getId();
    }

    @Override
    public int hashCode() {
        return HashCode.fromLong(getId()).hashCode();
    }
}
