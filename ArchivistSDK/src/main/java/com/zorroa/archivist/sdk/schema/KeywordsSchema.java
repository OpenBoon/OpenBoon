package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.*;

/**
 * A Schema class for managing keywords.
 */
public class KeywordsSchema implements Schema {

    /**
     * The maximum confidence value a keyword can have.
     */
    public static final double CONFIDENCE_MAX = 1.0;

    /**
     * The number of buckets the keywords are put into.
     */
    public static final int BUCKET_COUNT = 5;

    private Map<String, Set<String>> fields = Maps.newHashMap();
    private Set<String> all = Sets.newHashSet();
    private Set<String> suggest = Sets.newHashSet();

    /**
     * Add a new keyword with the given confidence.  Keywords with a confidence
     * value les than or equal to 0 are ignored.
     *
     * Optionally adds valid keywords to suggestion keywords as well, however
     * the confidence value should still be greater than 0 for this to happen.
     *
     * @param confidence
     * @param suggestion
     * @param keywords
     */
    public void addKeywords(double confidence, boolean suggestion, String... keywords) {
        if (confidence <= 0) {
            return;
        }
        addKeywords(confidence, suggestion, Arrays.asList(keywords));
    }

    /**
     * Add a new keyword with the given confidence.  Keywords with a confidence
     * value les than or equal to 0 are ignored.
     *
     * Optionally adds valid keywords to suggestion keywords as well, however
     * the confidence value should still be greater than 0 for this to happen.
     *
     * @param confidence
     * @param suggestion
     * @param keywords
     */
    public void addKeywords(double confidence, boolean suggestion, Iterable<String> keywords) {
        if (confidence <= 0) {
            return;
        }

        String field = String.format("level%d", getBucket(Math.min(confidence, CONFIDENCE_MAX)));
        Set<String> bucket = fields.get(field);

        if (bucket == null) {
            bucket = Sets.newHashSet();
            fields.put(field, bucket);
        }

        for (String word: keywords) {
            bucket.add(word);
            all.add(word);
            if (suggestion) {
                suggest.add(word);
            }
        }
    }

    /**
     * A given keywords to suggestion keywords field.
     *
     * @param keywords
     */
    public void addSuggestKeywords(String ... keywords) {
        suggest.addAll(Arrays.asList(keywords));
    }

    /**
     * A given keywords to suggestion keywords field.
     *
     * @param keywords
     */
    public void addSuggest(Collection<String> keywords) {
        suggest.addAll(keywords);
    }

    public void setAll(Set<String> allKeywords) {
        this.all = allKeywords;
    }

    public void setSuggest(Set<String> suggestKeywords) {
        this.suggest = suggestKeywords;
    }

    public Set<String> getAll() {
        return ImmutableSet.copyOf(all);
    }

    public Set<String> getSuggest() {
        return ImmutableSet.copyOf(suggest);
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

    public static long getBucket(double confidence) {
        return Math.max(1, Math.round(BUCKET_COUNT * (confidence / CONFIDENCE_MAX)));
    }
}
