package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Sets;
import com.zorroa.archivist.sdk.schema.KeywordsSchema;
import com.zorroa.archivist.sdk.schema.ExtendableSchema;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by chambers on 1/1/16.
 */
public class DocumentTests {

    @Test
    public void testSetAndGetNestedPrimitveAttr() {
        Document d = new Document();
        d.setAttr("a:b:c:str", "bizzle");
        d.setAttr("a:b:c:int", 10);
        d.setAttr("a:b:c:float", 3.2f);

        String s = d.getAttr("a:b:c:str");
        int i = d.getAttr("a:b:c:int");
        float f = d.getAttr("a:b:c:float");

        assertEquals("bizzle", s);
        assertEquals(10, i);
        assertEquals(3.2, f, 0.01);
    }

    @Test
    public void testSetJavaBean() {
        Bean b = new Bean();
        b.setCount(100);
        b.setName("gandalf");

        Document d = new Document();
        d.setAttr("a:b:c", b);

        Bean bean1 = d.getAttr("a:b:c");
        Bean bean2 = d.getAttr("a:b:c", Bean.class);

        assertEquals(bean1.count, bean2.count);
        assertEquals(bean1.name, bean2.name);
        assertEquals("gandalf", bean2.name);
        assertEquals(100, bean2.count);
    }

    @Test
    public void testSetJavaBeanProperty() {
        Bean b = new Bean();
        b.setCount(100);
        b.setName("gandalf");

        Document d = new Document();
        d.setAttr("a:b:c", b);
        d.setAttr("a:b:c:name", "hank");

        Bean bean = d.getAttr("a:b:c");
        assertEquals("hank", bean.getName());
    }

    @Test
    public void testSetArbitraryProperty() {
        BeanMap k = new BeanMap();
        Document d = new Document();
        d.setAttr("a:b:bean", k);
        d.setAttr("a:b:bean:words", Sets.newHashSet("word"));

        Set<String> words = d.getAttr("a:b:bean:words");
        assertTrue(words.contains("word"));
    }

    @Test
    public void testSetMapBeanPropertyWithSetter() {
        Set<String> words = Sets.newHashSet();
        words.add("foo");
        words.add("bingle");

        KeywordsSchema k = new KeywordsSchema();
        Document d = new Document();
        d.setAttr("a:b:keywords", k);
        d.setAttr("a:b:keywords:all", words);

        Set<String> words2 = d.getAttr("a:b:keywords:all");
        assertEquals(words, words2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetMapSchemaPropertyFailure() {
        BeanMap map = new BeanMap();
        Document d = new Document();
        d.setAttr("a:b:bean", map);
        // Cannot set this because foo is a string.
        d.setAttr("a:b:bean:foo", 1234);
    }

    @Test
    public void testGetNullValue() {
        Document d = new Document();
        Bean bean = d.getAttr("a:b:c");
        assertNull(bean);
    }

    @Test
    public void testRemovePropertyWithSetter() {
        Bean b = new Bean();
        b.setName("foo");
        Document d = new Document();
        d.setAttr("a:bean", b);
        d.removeAttr("a:bean:name");
        assertNull(b.getName());
    }

    @Test
    public void testRemoveArbitraryAttr() {
        BeanMap b = new BeanMap();
        b.setName("foo");
        Document d = new Document();
        d.setAttr("a:bean", b);
        d.setAttr("a:bean:bing", "bar");

        assertEquals("bar", b.any().get("bing"));
        d.removeAttr("a:bean:bing");
        assertNull(b.any().get("bing"));
    }

    public static class Bean {
        private String name;
        private int count;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public static class BeanMap extends ExtendableSchema {
        private String name;
        private int count;
        private String foo;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
