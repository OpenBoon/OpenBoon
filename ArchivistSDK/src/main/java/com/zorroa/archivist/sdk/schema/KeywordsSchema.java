package com.zorroa.archivist.sdk.schema;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * A Schema class for managing keywords.
 */
public class KeywordsSchema extends MapSchema {

    /**
     * The maximum confidence value a keyword can have.
     */
    public static final double CONFIDENCE_MAX = 1.0;

    /**
     * The number of buckets the keywords are put into.
     */
    public static final int BUCKET_COUNT = 5;

    private Set<String> all = Sets.newHashSet();
    private Set<String> suggest = Sets.newHashSet();


    /**
     * Add an array of keywords to the highest confidence and suggestion words
     * with the given type.
     *
     * @param type
     * @param keywords
     */
    public void addKeywords(String type,  String ... keywords) {
        addKeywords(type, Arrays.asList(keywords));
    }

    /**
     * Add an array of keywords to the highest confidence and suggestion words
     * with the given type.
     *
     * @param type
     * @param keywords
     */
    public void addKeywords(String type,  Iterable<String> keywords) {
        Set<String> words = getAttr(type);
        if (words == null) {
            words = Sets.newHashSet();
            setAttr(type, words);
        }
        addKeywords(1, true, keywords);
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
        Set<String> bucket = getAttr(field);

        if (bucket == null) {
            bucket = Sets.newHashSet();
            setAttr(field, bucket);
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

    public static long getBucket(double confidence) {
        return Math.max(1, Math.round(BUCKET_COUNT * (confidence / CONFIDENCE_MAX)));
    }
}
