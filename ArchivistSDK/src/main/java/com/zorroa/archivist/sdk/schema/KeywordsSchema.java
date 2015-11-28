package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.*;

/**
 * Created by chambers on 11/24/15.
 */
public class KeywordsSchema implements Schema {

    private Map<String, Set<String>> fields = Maps.newHashMap();
    private Set<String> allKeywords = Sets.newHashSet();
    private Set<String> suggestKeywords = Sets.newHashSet();

    public void addKeywords(int confidence, boolean suggest, String... keywords) {
        String field = String.format("level%03d", confidence);
        Set<String> bucket = fields.get(field);

        if (bucket == null) {
            bucket = Sets.newHashSet();
            fields.put(field, bucket);
        }
        List<String> allValues = Arrays.asList(keywords);
        bucket.addAll(allValues);
        allKeywords.addAll(allValues);
        if (suggest) {
            suggestKeywords.addAll(allValues);
        }
    }

    public void setAllKeywords(Set<String> allKeywords) {
        this.allKeywords = allKeywords;
    }

    public void setSuggestKeywords(Set<String> suggestKeywords) {
        this.suggestKeywords = suggestKeywords;
    }

    @JsonProperty("all")
    public Set<String> getAllKeywords() {
        return ImmutableSet.copyOf(allKeywords);
    }

    @JsonProperty("suggest")
    public Set<String> getSuggestKeywords() {
        return ImmutableSet.copyOf(suggestKeywords);
    }

    @Override
    public String getNamespace() {
        return "keywords";
    }

    @JsonAnyGetter
    public Map<String,Set<String>> any() {
        return fields;
    }

    @JsonAnySetter
    public void set(String name,  Set<String> value) {
        fields.put(name, value);
    }
}
