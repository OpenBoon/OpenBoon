package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Setting;
import com.zorroa.archivist.domain.SettingsFilter;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by chambers on 5/30/17.
 */
public class SettingsServiceTests extends AbstractTest {

    @Autowired
    SettingsService settingsService;

    @Test
    public void testSetAll() {
        settingsService.setAll(ImmutableMap.of(
                "archivist.search.keywords.static.fields", "{'foo.bar': 1.5}"));
        Map<String, Float> fields = searchService.getQueryFields();
        assertTrue(fields.containsKey("foo.bar"));
        assertEquals(1.5, fields.get("foo.bar"), 0.1);
    }

    @Test
    public void testSet() {
        settingsService.set("archivist.search.keywords.static.fields", "{'foo.bar': 1.5}");
        Map<String, Float> fields = searchService.getQueryFields();
        assertTrue(fields.containsKey("foo.bar"));
        assertEquals(1.5, fields.get("foo.bar"), 0.1);
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetAllIllegalProperty() {
        settingsService.setAll(ImmutableMap.of("archivist.blah", "1.0"));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetIllegalType() {
        settingsService.setAll(ImmutableMap.of(
                "archivist.search.keywords.auto.enabled", "Boing!"));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetIllegalMapType() {
        settingsService.setAll(ImmutableMap.of(
                "archivist.search.keywords.static.fields",
                "{'foo.bar': 'rabbit'}"));
    }

    @Test
    public void testGetAll() {
        List<Setting> settings = settingsService.getAll();
        assertFalse(settings.isEmpty());
    }

    @Test
    public void testGetAllPrefixFilter() {
        List<Setting> settings = settingsService.getAll(new SettingsFilter().setStartsWith(
                ImmutableSet.of("server")
        ));
        assertEquals(12, settings.size());
    }

    @Test
    public void testGet() {
        String name = "archivist.search.keywords.auto.enabled";
        Setting setting = settingsService.get(name);
        assertEquals(name, setting.getName());
        assertEquals("Search Settings", setting.getCategory());
        assertEquals("Automatically detect and utilize fields in keyword searches.",
                setting.getDescription());
        assertTrue(setting.isLive());
        assertEquals("true", setting.getCurrentValue());
        assertEquals("true", setting.getDefaultValue());
        assertTrue(setting.isDefault());
    }

    @Test
    public void testGetAllWithLimit() {
        List<Setting> settings = settingsService.getAll(
                new SettingsFilter()
                        .setStartsWith(ImmutableSet.of("server."))
                        .setCount(2));
        assertEquals(2, settings.size());
    }

    @Test
    public void testGetAllWithNameFilter() {
        List<Setting> settings = settingsService.getAll();
        assertFalse(settings.isEmpty());
    }
}
