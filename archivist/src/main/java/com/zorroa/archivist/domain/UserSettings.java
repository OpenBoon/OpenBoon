package com.zorroa.archivist.domain;

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
    private Metadata metadata;

    public Search getSearch() {
        return search;
    }

    public UserSettings setSearch(Search search) {
        this.search = search;
        return this;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public UserSettings setMetadata(Metadata metadata) {
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

    /**
     * All metadata related display setting.  This mostly revolves around
     * what attributes to display in the metadata window.
     *
     * This class is currently a place holder until the structure of this
     * setting is known.
     */
    public static class Metadata {

    }
}
