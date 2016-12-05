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
    private Search search;

    /**
     * All settings related to display of metadata.
     */
    private Map<String, Object> metadata;

    public UserSettings() {
        this.search = new Search();
        this.metadata = Maps.newHashMap();
    }

    public Search getSearch() {
        return search;
    }

    public UserSettings setSearch(Search search) {
        this.search = search;
        return this;
    }

    public Map<String,Object> getMetadata() {
        return metadata;
    }

    public UserSettings setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * All search related settings.
     */
    public static class Search {

        /**
         * The fields and their associated boost value, which are used with
         * a query string query.
         */
        private Map<String, Float> queryFields;

        public Map<String, Float> getQueryFields() {
            return queryFields;
        }

        public Search setQueryFields(Map<String, Float> queryFields) {
            this.queryFields = queryFields;
            return this;
        }
    }
}
