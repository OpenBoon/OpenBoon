package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Setting;
import com.zorroa.archivist.domain.SettingsFilter;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 5/30/17.
 */
public class SettingsServiceTests extends AbstractTest {

    @Test
    public void testSetAll() {
        settingsService.setAll(ImmutableMap.of(
                "archivist.export.dragTemplate",
                "bar"));
        assertEquals("bar", settingsService.get("archivist.export.dragTemplate")
                .getCurrentValue());
        settingsService.set("archivist.export.dragTemplate", null);
    }

    @Test
    public void testSet() {
        settingsService.set("archivist.export.dragTemplate", "foo.bar:1.5");
        assertEquals("foo.bar:1.5",
                settingsService.get("archivist.export.dragTemplate").getCurrentValue());
        settingsService.set("archivist.export.dragTemplate", null);
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetAllIllegalProperty() {
        settingsService.setAll(ImmutableMap.of("archivist.blah", "1.0"));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetRegexFailure() {
        assertEquals("true",
                settingsService.get("archivist.search.keywords.auto.enabled").getCurrentValue());
        settingsService.setAll(ImmutableMap.of(
                "archivist.search.keywords.auto.enabled", "Boing!"));
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
