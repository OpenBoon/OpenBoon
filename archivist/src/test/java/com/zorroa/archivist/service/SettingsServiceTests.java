package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Setting;
import com.zorroa.archivist.domain.SettingsFilter;
import com.zorroa.common.domain.ArchivistWriteException;
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
                "curator.thumbnails.drag-template",
                "bar"));
        assertEquals("bar", settingsService.get("curator.thumbnails.drag-template")
                .getCurrentValue());
        settingsService.set("curator.thumbnails.drag-template", null);
    }

    @Test
    public void testSet() {
        settingsService.set("curator.thumbnails.drag-template", "foo.bar:1.5");
        assertEquals("foo.bar:1.5",
                settingsService.get("curator.thumbnails.drag-template").getCurrentValue());
        settingsService.set("curator.thumbnails.drag-template", null);
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetAllIllegalProperty() {
        settingsService.setAll(ImmutableMap.of("archivist.blah", "1.0"));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetRegexFailure() {
        settingsService.setAll(ImmutableMap.of(
                "archivist.search.keywords.boost", "Boing!"));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testIntRegexValidationError() {
        settingsService.setAll(ImmutableMap.of(
                "curator.lightbox.zoom-min", "bong!"));
    }

    @Test
    public void testIntRegexValidation() {
        settingsService.setAll(ImmutableMap.of(
                "curator.lightbox.zoom-min", "100"));
        Setting value = settingsService.get("curator.lightbox.zoom-min");
        assertEquals("100", value.getCurrentValue());

    }

    @Test
    public void testGetAll() {
        List<Setting> settings = settingsService.getAll();
        assertFalse(settings.isEmpty());
    }

    @Test
    public void testGetAllPrefixFilter() {
        SettingsFilter filter = new SettingsFilter();
        filter.setStartsWith(ImmutableSet.of("server"));
        List<Setting> settings = settingsService.getAll(filter);
        assertEquals(9, settings.size());
    }

    @Test
    public void testGet() {
        String name = "archivist.search.sortFields";
        Setting setting = settingsService.get(name);
        assertEquals(name, setting.getName());
        assertEquals("Search Settings", setting.getCategory());
        assertEquals("The default sort fields in the format of field:direction,field:direction. Score is always first.",
                setting.getDescription());
        assertTrue(setting.isLive());
        assertEquals("zorroa.timeCreated:DESC", setting.getCurrentValue());
        assertEquals("zorroa.timeCreated:DESC", setting.getDefaultValue());
        assertTrue(setting.isDefault());
    }

    @Test
    public void testGetAllWithLimit() {
        SettingsFilter filter = new SettingsFilter();
        filter.setStartsWith(ImmutableSet.of("server."));
        filter.setCount(2);
        List<Setting> settings = settingsService.getAll(filter);
        assertEquals(2, settings.size());
    }

    @Test
    public void testGetAllWithNameFilter() {
        List<Setting> settings = settingsService.getAll();
        assertFalse(settings.isEmpty());
    }
}
