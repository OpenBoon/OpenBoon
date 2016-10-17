package com.zorroa.archivist.domain;

import java.util.Map;

/**
 * Created by chambers on 10/17/16.
 */
public class UserSettings {

    private Search search;
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

    public static class Search {
        private Map<String, Float> queryFields;

        public Map<String, Float> getQueryFields() {
            return queryFields;
        }

        public Search setQueryFields(Map<String, Float> queryFields) {
            this.queryFields = queryFields;
            return this;
        }
    }

    public static class Metadata {

    }
}
