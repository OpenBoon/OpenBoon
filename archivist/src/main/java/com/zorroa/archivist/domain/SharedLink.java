package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by chambers on 7/10/17.
 */
public class SharedLink {

    private int id;
    private Map<String, Object> state;
    private Set<Integer> userIds;
    private long expireTime;

    public int getId() {
        return id;
    }

    public SharedLink setId(int id) {
        this.id = id;
        return this;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public SharedLink setState(Map<String, Object> state) {
        this.state = state;
        return this;
    }

    public Set<Integer> getUserIds() {
        return userIds;
    }

    public SharedLink setUserIds(Set<Integer> userIds) {
        this.userIds = userIds;
        return this;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public SharedLink setExpireTime(long expireTime) {
        this.expireTime = expireTime;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("state", state)
                .add("userIds", userIds)
                .add("expireTime", expireTime)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedLink that = (SharedLink) o;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
