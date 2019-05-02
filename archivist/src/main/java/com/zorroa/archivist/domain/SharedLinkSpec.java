package com.zorroa.archivist.domain;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by chambers on 7/10/17.
 */
public class SharedLinkSpec {

    @NotEmpty
    private Map<String, Object> state;

    private Set<UUID> userIds;
    private boolean sendEmail = false;
    private Long expireTimeMs;

    public Map<String, Object> getState() {
        return state;
    }

    public SharedLinkSpec setState(Map<String, Object> state) {
        this.state = state;
        return this;
    }

    public Set<UUID> getUserIds() {
        return userIds;
    }

    public SharedLinkSpec setUserIds(Set<UUID> userIds) {
        this.userIds = userIds;
        return this;
    }

    public boolean isSendEmail() {
        return sendEmail;
    }

    public SharedLinkSpec setSendEmail(boolean sendEmail) {
        this.sendEmail = sendEmail;
        return this;
    }

    public Long getExpireTimeMs() {
        return expireTimeMs;
    }

    public SharedLinkSpec setExpireTimeMs(Long expireTimeMs) {
        this.expireTimeMs = expireTimeMs;
        return this;
    }
}
