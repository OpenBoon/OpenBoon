package com.zorroa.archivist.sdk.schema;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Defines a set of relationships between documents.
 */
public class LinkSchema extends ExtendableSchema<String, Set<String>> {

    /**
     * A set of parent assets. This would normally be a single ID but we allow
     * multiple.
     */
    private Set<String> parents;

    /**
     * A set of file paths derived or extracted from an asset.
     */
    private Set<String> derived;

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

    public Set<String> getDerived() {
        return derived;
    }

    public LinkSchema setDerived(Set<String> derived) {
        this.derived = derived;
        return this;
    }

    public LinkSchema addToDerived(String path) {
        if (this.derived == null) {
            this.derived = Sets.newHashSet();
        }
        this.derived.add(path);
        return this;
    }

}
