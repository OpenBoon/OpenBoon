package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 5/30/17.
 */
public class SettingsDaoTests extends AbstractTest {

    @Autowired
    SettingsDao settingsDao;

    @After
    public void after() {
        System.clearProperty("foo.bar");
    }

    @Test
    public void testSet() {
        settingsDao.set("foo.bar", 1);
        assertEquals("1", settingsDao.get("foo.bar"));
    }

    @Test
    public void testGetAll() {
        settingsDao.set("foo.bar", 1);
        Map<String,String> all = settingsDao.getAll();
        assertEquals("1", all.get("foo.bar"));
    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void testUnset() {
        settingsDao.set("foo.bar", 1);
        assertEquals("1", settingsDao.get("foo.bar"));
        settingsDao.unset("foo.bar");
        settingsDao.get("foo.bar");
    }


}
