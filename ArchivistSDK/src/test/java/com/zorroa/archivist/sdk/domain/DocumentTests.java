package com.zorroa.archivist.sdk.domain;

import com.zorroa.archivist.sdk.schema.KeywordsSchema;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 1/1/16.
 */
public class DocumentTests {

    @Test
    public void testGetAttrSimple() {
        Document d = new Document();
        assertEquals("bing", d.setAttr("foo", "bar", "bing").getAttr("foo.bar"));

        String[] value = new String[] { "a", "b", "c"};
        assertTrue(Arrays.equals(value,  d.setAttr("foo", "bar", value).getAttr("foo.bar")));
    }

    @Test
    public void testGetAttr() {
        Document d = new Document();
        assertEquals("bing", d.setAttr("foo", "bar", "bing").getAttr("foo","bar"));

        String[] value = new String[] { "a", "b", "c"};
        assertTrue(Arrays.equals(value,  d.setAttr("foo", "bar", value).getAttr("foo","bar")));
    }

    @Test
    public void testGetEnumAttr() {
        Document d = new Document();
        d.setAttr("foo", "bar", AssetType.Image);
        assertEquals(AssetType.Image, d.getAttr("foo.bar"));
    }

    @Test
    public void testGetSchema() {
        Document d = new Document();

        KeywordsSchema keywords = new KeywordsSchema();
        keywords.addKeywords(1.0, false, "foosball");
        d.addSchema(keywords);

        KeywordsSchema keywords2 = d.getSchema("keywords", KeywordsSchema.class);
        assertEquals(keywords.getAll(), keywords2.getAll());
    }

    @Test
    public void testSetSchemaWithGetAttr() {
        Document d = new Document();

        KeywordsSchema keywords = new KeywordsSchema();
        keywords.addKeywords(1.0, false, "foosball");
        d.addSchema(keywords);

        Set<String> all = d.getAttr("keywords.all");
        assertTrue(all.contains("foosball"));
    }
}
