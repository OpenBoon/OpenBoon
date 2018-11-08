package com.zorroa.archivist.service

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Setting
import com.zorroa.archivist.domain.SettingsFilter
import com.zorroa.common.domain.ArchivistWriteException
import org.junit.Test

import org.junit.Assert.*

/**
 * Created by chambers on 5/30/17.
 */
class SettingsServiceTests : AbstractTest() {

    @Test
    fun testSetAll() {
        settingsService.setAll(ImmutableMap.of(
                "curator.thumbnails.drag-template",
                "bar"))
        assertEquals("bar", settingsService.get("curator.thumbnails.drag-template")
                .currentValue)
        settingsService.set("curator.thumbnails.drag-template", null)
    }

    @Test
    fun testSet() {
        settingsService.set("curator.thumbnails.drag-template", "foo.bar:1.5")
        assertEquals("foo.bar:1.5",
                settingsService.get("curator.thumbnails.drag-template").currentValue)
        settingsService.set("curator.thumbnails.drag-template", null)
    }

    @Test(expected = ArchivistWriteException::class)
    fun testSetAllIllegalProperty() {
        settingsService.setAll(ImmutableMap.of("archivist.blah", "1.0"))
    }

    @Test(expected = ArchivistWriteException::class)
    fun testSetRegexFailure() {
        settingsService.setAll(ImmutableMap.of(
                "archivist.search.keywords.boost", "Boing!"))
    }

    @Test(expected = ArchivistWriteException::class)
    fun testIntRegexValidationError() {
        settingsService.setAll(ImmutableMap.of(
                "curator.lightbox.zoom-min", "bong!"))
    }

    @Test
    fun testIntRegexValidation() {
        settingsService.setAll(ImmutableMap.of(
                "curator.lightbox.zoom-min", "100"))
        val value = settingsService.get("curator.lightbox.zoom-min")
        assertEquals("100", value.currentValue)

    }

    @Test
    fun testGetAll() {
        val settings = settingsService.getAll()
        assertFalse(settings.isEmpty())
    }

    @Test
    fun testGetAllPrefixFilter() {
        val filter = SettingsFilter()
        filter.startsWith = ImmutableSet.of("server")
        val settings = settingsService.getAll(filter)
        for (setting in settings) {
            assertTrue(setting.name.startsWith("server."))
        }
    }

    @Test
    fun testGet() {
        val name = "archivist.search.sortFields"
        val setting = settingsService.get(name)
        assertEquals(name, setting.name)
        assertEquals("Search Settings", setting.category)
        assertEquals("The default sort fields in the format of field:direction,field:direction. Score is always first.",
                setting.description)
        assertTrue(setting.isLive)
        assertEquals("system.timeCreated:DESC", setting.currentValue)
        assertEquals("system.timeCreated:DESC", setting.defaultValue)
        assertTrue(setting.isDefault)
    }

    @Test
    fun testGetAllWithLimit() {
        val filter = SettingsFilter()
        filter.startsWith = ImmutableSet.of("server.")
        filter.count = 2
        val settings = settingsService.getAll(filter)
        assertEquals(2, settings.size.toLong())
    }

    @Test
    fun testGetAllWithNameFilter() {
        val settings = settingsService.getAll()
        assertFalse(settings.isEmpty())
    }
}
