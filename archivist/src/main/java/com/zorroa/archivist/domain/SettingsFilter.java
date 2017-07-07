package com.zorroa.archivist.domain;

import com.amazonaws.util.CollectionUtils;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Created by chambers on 7/6/17.
 */
public class SettingsFilter {

    /**
     * Only return "live settings", which are settings that can be modified.
     */
    private boolean liveOnly;

    /**
     * A set of exact setting names to return.
     */
    private Set<String> names = ImmutableSet.of();

    /**
     * A set of startsWith filters to match setting names against.
     */
    private Set<String> startsWith = ImmutableSet.of();

    /**
     * The maximum number of settings to return.
     */
    private int count = Integer.MAX_VALUE;

    public boolean isLiveOnly() {
        return liveOnly;
    }

    public SettingsFilter setLiveOnly(boolean liveOnly) {
        this.liveOnly = liveOnly;
        return this;
    }

    public Set<String> getNames() {
        return names;
    }

    public SettingsFilter setNames(Set<String> names) {
        this.names = names;
        return this;
    }

    public boolean matches(Setting setting) {
        if (!CollectionUtils.isNullOrEmpty(names)) {
            if (!names.contains(setting.getName())) {
                return false;
            }
        }
        if (!CollectionUtils.isNullOrEmpty(startsWith)) {
            boolean match = false;
            for (String prefix: startsWith) {
                if (setting.getName().startsWith(prefix)) {
                    match =true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }

        if (liveOnly) {
            if (!setting.isLive()) {
                return false;
            }
        }

        return true;
    }

    public int getCount() {
        return count;
    }

    public SettingsFilter setCount(int count) {
        this.count = count;
        return this;
    }

    public Set<String> getStartsWith() {
        return startsWith;
    }

    public SettingsFilter setStartsWith(Set<String> startsWith) {
        this.startsWith = startsWith;
        return this;
    }
}
