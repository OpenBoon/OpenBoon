package com.zorroa.archivist.domain;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Created by chambers on 8/9/16.
 */
public class FilterSpec {

    @NotEmpty
    private String description;

    private boolean enabled = true;

    private boolean matchAll = true;

    /**
     * Can optionally be populated with matchers.
     */
    private List<MatcherSpec> matchers;

    /**
     * Can optionally be populated with actions.
     */
    private List<ActionSpec> actions;

    public String getDescription() {
        return description;
    }

    public FilterSpec setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<MatcherSpec> getMatchers() {
        return matchers;
    }

    public FilterSpec setMatchers(List<MatcherSpec> matchers) {
        this.matchers = matchers;
        return this;
    }

    public List<ActionSpec> getActions() {
        return actions;
    }

    public FilterSpec setActions(List<ActionSpec> actions) {
        this.actions = actions;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public FilterSpec setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    public FilterSpec setMatchAll(boolean matchAll) {
        this.matchAll = matchAll;
        return this;
    }


}
