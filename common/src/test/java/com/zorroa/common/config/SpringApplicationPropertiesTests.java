package com.zorroa.common.config;

import com.google.common.collect.ImmutableList;
import com.zorroa.common.UnitTestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 2/17/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=UnitTestConfiguration.class)
@TestPropertySource("classpath:/test.properties")
public class SpringApplicationPropertiesTests {

    @Autowired
    SpringApplicationProperties properties;

    @Test
    public void testGet() {
        assertEquals("abc", properties.get("test.prop.string", String.class));
        assertEquals(123, (int) properties.get("test.prop.int", Integer.class));
        assertEquals(3.14159, (double)properties.get("test.prop.double", Double.class), 0.01);
        assertEquals(true, properties.get("test.prop.bool", Boolean.class));
    }

    @Test
    public void testGetDefault() {
        assertEquals("bar", properties.get("test.prop.string_foo", "bar"));
        assertEquals(1000, (int) properties.get("test.prop.int_foo", 1000));
        assertEquals(1.21, (double)properties.get("test.prop.double_foo", 1.21), 0.01);
        assertEquals(false, properties.get("test.prop.bool_foo", false));
    }

    @Test
    public void testGetString() {
        assertEquals("abc", properties.getString("test.prop.string"));
        assertEquals("bar", properties.getString("test.prop.string_foo", "bar"));
    }

    @Test
    public void getListInFile() {
        List<String> items = properties.getList("test.list");
        assertEquals(ImmutableList.of("pencil", "pen", "marker"), items);
    }

    @Test
    public void getEmptyList() {
        List<String> items = properties.getList("test.emptyList");
        assertTrue(items.isEmpty());
    }

    @Test
    public void getList() {
        List<String> items = properties.getList("test.arrayList");
        assertEquals(3, items.size());
        assertEquals(ImmutableList.of("a","b","c"), items);
    }

    @Test
    public void getInt() {
        assertEquals(123, properties.getInt("test.prop.int"));
        assertEquals(1000, properties.getInt("test.prop.string_foo", 1000));
    }

    @Test
    public void getDouble() {
        assertEquals(3.14159, properties.getDouble("test.prop.double"), 0.0001);
        assertEquals(1.21, properties.getDouble("test.prop.double_foo", 1.21), 0.0001);
    }

    @Test
    public void getBoolean() {
        assertEquals(true, properties.getBoolean("test.prop.bool"));
        assertEquals(false, properties.getBoolean("test.prop.bool_foo", false));
    }

    @Test
    public void getMap() {
        Map<String, Object> map = properties.getMap("test.prop");
        assertEquals(4, map.size());
        assertEquals("abc", map.get("test.prop.string"));
        assertEquals("123", map.get("test.prop.int"));
        assertEquals("3.14159", map.get("test.prop.double"));
        assertEquals("true", map.get("test.prop.bool"));
    }

    @Test
    public void getProperties() {
        Properties props = properties.getProperties("test.prop");
        assertEquals(4, props.size());
        assertEquals("abc", props.get("test.prop.string"));
        assertEquals("123", props.get("test.prop.int"));
        assertEquals("3.14159", props.get("test.prop.double"));
        assertEquals("true", props.get("test.prop.bool"));
    }

    @Test
    public void getPropertiesNoPrefix() {
        Properties props = properties.getProperties("test.prop.", false);
        //assertEquals(4, props.size());
        assertEquals("abc", props.get("string"));
        assertEquals("123", props.get("int"));
        assertEquals("3.14159", props.get("double"));
        assertEquals("true", props.get("bool"));
    }
}
