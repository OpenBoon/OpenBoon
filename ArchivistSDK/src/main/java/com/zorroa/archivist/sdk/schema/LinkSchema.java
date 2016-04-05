package com.zorroa.archivist.sdk.schema;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by chambers on 3/25/16.
 */
public class LinkSchema extends ExtendableSchema<String, Set<String>> {

    private Set<String> parents;

    @Override
    public Set<String> getDefaultValue() {
        return Sets.newHashSet();
    }

    public LinkSchema addToParents(String parent) {
        if (parents == null) {
            parents = Sets.newHashSet();
        }
        parents.add(parent);
        return this;
    }

    public Set<String> getParents() {
        return parents;
    }

    public LinkSchema setParents(Set<String> parents) {
        this.parents = parents;
        return this;
    }
}
