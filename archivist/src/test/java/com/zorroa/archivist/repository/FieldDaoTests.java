package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FieldDaoTests extends AbstractTest {

    @Autowired
    FieldDao fieldDao;

    @Test
    public void testAddIgnoreField() {
        assertTrue(fieldDao.hideField("foo.bar.bing", true));
        fieldDao.hideField("foo.bar.bing", true);
        assertEquals(1, (int)
                jdbc.queryForObject("SELECT COUNT(1) FROM field_hide", Integer.class));
    }

    @Test
    public void testUnignoreField() {
        assertTrue(fieldDao.hideField("foo.bar.bing", true));
        assertTrue(fieldDao.unhideField("foo.bar.bing"));
        assertFalse(fieldDao.unhideField("foo.bar.bing"));
    }
}
