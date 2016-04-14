package com.zorroa.archivist.sdk.schema;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 4/13/16.
 */
public class KeywordsSchemaTests {

    private static final Logger logger = LoggerFactory.getLogger(KeywordsSchemaTests.class);

    @Test
    public void testAddSuggestKeywordsCollection() {
        KeywordsSchema schema = new KeywordsSchema();
        schema.addSuggestKeywords("foo", ImmutableList.of("test"));
        assertTrue(schema.getSuggest().contains("test"));
    }

    @Test
    public void testAddSuggestKeywordsArray() {
        KeywordsSchema schema = new KeywordsSchema();
        schema.addSuggestKeywords("foo", "testing", "one", "two");
        assertTrue(schema.getSuggest().contains("testing"));
        assertTrue(schema.getSuggest().contains("one"));
        assertTrue(schema.getSuggest().contains("two"));
        assertFalse(schema.getSuggest().contains("three"));
    }


    @Test
    public void testKeywordsCollection() {
        KeywordsSchema schema = new KeywordsSchema();
        schema.addKeywords("foo", ImmutableList.of("test"));
        assertTrue(schema.any().get("foo").contains("test"));
    }

    @Test
    public void testKeywordsArray() {
        KeywordsSchema schema = new KeywordsSchema();
        schema.addKeywords("foo", "testing", "one", "two");
        assertTrue(schema.any().get("foo").contains("testing"));
    }

}
