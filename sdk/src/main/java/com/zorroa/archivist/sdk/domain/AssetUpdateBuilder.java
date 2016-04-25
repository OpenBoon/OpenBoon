package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Sets;
import com.zorroa.archivist.sdk.schema.PermissionSchema;

import java.util.Set;

/**
 * The asset update builder doesn't let you just update any part of the asset for a few reasons:
 *
 * - The data will likely be overwritten by an ingest.
 * - It could be used to remove things like export IDS or ingest information.
 *
 * So for now we'll use this object as a way to expose fields we do want people to change,
 * except for folders which are handled specifically with their own API calls.
 */
public class AssetUpdateBuilder {

    /**
     * Set a list of valid permission IDs for each type of permission, looks like:
     * {
     *    "write": [1,3,4],
     *    "search": [2],
     *    "export": [10]
     * }
     *
     */
    private PermissionSchema permissions;
    private Integer rating;

    Set<String> isset = Sets.newHashSet();

    public PermissionSchema getPermissions() {
        return permissions;
    }

    public AssetUpdateBuilder setPermissions(PermissionSchema permissions) {
        this.permissions = permissions;
        isset.add("permissions");
        return this;
    }

    public boolean isset(String name) {
        return isset.contains(name);
    }

    public Integer getRating() {
        return rating;
    }

    public AssetUpdateBuilder setRating(Integer rating) {
        if (rating.intValue() > 5) {
            rating = 5;
        }
        if (rating.intValue()< 0) {
            rating = 0;
        }

        this.rating = rating;
        isset.add("rating");
        return this;
    }

}
