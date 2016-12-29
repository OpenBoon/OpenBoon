package com.zorroa.archivist.domain;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Class to represent user customizable settings.  See property comments for a full
 * description of each field.
 *
 * Example JSON structure:
 *
 * {
 *     "search": {
 *         "queryFields": {
 *             "source.filename": 1.0,
 *             "keywords.all", 2.0
 *         }
 *     },
 *     "metadata": {
 *
 *     }
 * }
 *
 */
public class UserSettings {

    /**
     * All search related settings.
     */
    private Map<String, Object> search;

    /**
     * All settings related to display of metadata.
     */
    private Map<String, Object> metadata;

    public UserSettings() {
        this.search = Maps.newHashMap();
        this.metadata = Maps.newHashMap();
    }

    public Map<String,Object> getMetadata() {
        return metadata;
    }

    public UserSettings setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Map<String, Object> getSearch() {
        return search;
    }

    public UserSettings setSearch(Map<String, Object> search) {
        this.search = search;
        return this;
    }
}
