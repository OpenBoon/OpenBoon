package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 5/30/17.
 */
public class SettingsServiceTests extends AbstractTest {

    @Autowired
    SettingsService settingsService;

    @Test
    public void testSetAll() {
        settingsService.setAll(ImmutableMap.of("archivist.search.keywords.field.foo.bar", 1.0));
        Map<String, Float> fields = searchService.getQueryFields();
        assertEquals(1.0, fields.get("foo.bar"), 0.001);
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetIllegalProperty() {
        settingsService.setAll(ImmutableMap.of("archivist.blah", 1.0));
    }

    @Test(expected=ArchivistWriteException.class)
    public void testSetIllegalType() {
        settingsService.setAll(ImmutableMap.of("archivist.search.keywords.auto.enabled", "Boing!"));
    }

}
