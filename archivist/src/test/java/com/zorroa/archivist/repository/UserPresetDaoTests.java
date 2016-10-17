package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.UserPreset;
import com.zorroa.archivist.domain.UserPresetSpec;
import com.zorroa.archivist.domain.UserSettings;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 10/17/16.
 */
public class UserPresetDaoTests extends AbstractTest {

    @Autowired
    UserPresetDao userPresetDao;

    UserPresetSpec spec;
    UserPreset preset;

    @Before
    public void init()  {
        spec = new UserPresetSpec();
        spec.setName("defaults");
        spec.setSettings(new UserSettings().setSearch(
                new UserSettings.Search().setQueryFields(ImmutableMap.of("foo", 1.0f))));
        spec.setPermissionIds(ImmutableList.of(userService.getPermission("group::superuser").getId()));
        preset = userPresetDao.create(spec);
    }

    @Test
    public void testCreate() {
        assertEquals(spec.getName(), preset.getName());
        assertNotNull(preset.getPermissionIds());
        assertEquals(spec.getPermissionIds(), preset.getPermissionIds());
        assertTrue(spec.getSettings().getSearch().getQueryFields().containsKey("foo"));
    }

    @Test
    public void testDelete() {
        assertTrue(userPresetDao.delete(preset.getPresetId()));
    }

    @Test
    public void testUpdate() {
        preset.setPermissionIds(ImmutableList.of(userService.getPermission("group::manager").getId()));
        preset.setSettings(new UserSettings().setSearch(
                new UserSettings.Search().setQueryFields(ImmutableMap.of("bar", 1.0f))));
        preset.setName("bilbo");
        assertTrue(userPresetDao.update(preset.getPresetId(), preset));

        UserPreset updated = userPresetDao.refresh(preset);
        assertEquals(preset.getName(), updated.getName());
        assertNotNull(updated.getPermissionIds());
        assertEquals(preset.getPermissionIds(), updated.getPermissionIds());
        assertTrue(updated.getSettings().getSearch().getQueryFields().containsKey("bar"));
    }

    @Test
    public void testCount() {
        assertEquals(1, userPresetDao.count());
        spec.setName("shoe");
        userPresetDao.create(spec);
        assertEquals(2, userPresetDao.count());
    }

    @Test
    public void testExists() {
        assertTrue(userPresetDao.exists("defaults"));
        assertFalse(userPresetDao.exists("bar"));
    }

    @Test
    public void testGetAll() {
        List<UserPreset> presets = userPresetDao.getAll();
        assertEquals(1, presets.size());
    }
}
