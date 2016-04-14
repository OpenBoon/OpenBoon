package com.zorroa.archivist.sdk.schema;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * A Schema class for managing keywords.
 */
public class KeywordsSchema extends ExtendableSchema<String, Set<String>> {

    /**
     * Contains all the suggestion keywords.
     */
    private Set<String> suggest = Sets.newHashSet();

    @Override
    public Set<String> getDefaultValue() {
        return Sets.newHashSet();
    }

    /**
     * Add the given collection of keywords as a particular type of keywords.
     *
     * @param type
     * @param keywords
     */
    public void addKeywords(String type, Collection<String> keywords) {
        if (keywords.isEmpty()) {
            return;
        }
        Set<String> words = delegate.get(type);
        if (words == null) {
            words = Sets.newHashSet(keywords);
            delegate.put(type, words);
        }
        else {
            words.addAll(keywords);
        }
    }

    /**
     * Add the given array of keywords as a particular type of keywords.
     *
     * @param type
     * @param keywords
     */
    public void addKeywords(String type, String ... keywords) {
        addKeywords(type, Arrays.asList(keywords));
    }

    /**
     * Add the given collection of keywords as a particular type of keyword
     * as well as suggestion keywords.
     *
     * @param type
     * @param keywords
     */
    public void addSuggestKeywords(String type, Collection<String> keywords) {
        addKeywords(type, keywords);
        suggest.addAll(keywords);
    }

    /**
     * Add the given collection of keywords as a particular type of keyword
     * as well as suggestion keywords.
     *
     * @param type
     * @param keywords
     */
    public void addSuggestKeywords(String type, String ... keywords) {
        addKeywords(type, keywords);
        suggest.addAll(Arrays.asList(keywords));
    }

    public void addToSuggest(Set<String> keywords) {
        this.suggest.addAll(keywords);
    }

    public void setSuggest(Set<String> suggestKeywords) {
        this.suggest = suggestKeywords;
    }

    public Set<String> getSuggest() {
        return ImmutableSet.copyOf(suggest);
    }

    public Set<String> getAll() {
        Set<String> result = Sets.newHashSet(suggest);
        delegate.values().forEach(result::addAll);
        return result;
    }
}
