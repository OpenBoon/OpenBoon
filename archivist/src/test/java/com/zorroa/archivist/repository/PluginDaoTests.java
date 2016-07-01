package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.service.PluginService;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.PluginDao;
import com.zorroa.sdk.plugins.Plugin;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 6/30/16.
 */
public class PluginDaoTests extends AbstractTest {

    @Autowired
    PluginDao pluginDao;

    @Autowired
    PluginService pluginService;

    @Before
    public void init() {
        pluginService.registerAllPlugins();
        refreshIndex();
    }

    @Test
    public void testGetAll() {
        assertEquals(1, pluginDao.getAll().size());
    }

    @Test
    public void testGetAllWithPaging() {
        assertEquals(1, (long) pluginDao.getAll(new Paging(1)).getPage().getTotalCount());
        assertEquals(1, (long) pluginDao.getAll(new Paging(1)).getPage().getTotalPages());
        assertEquals(1, (long) pluginDao.getAll(new Paging(1)).getPage().getNumber());
    }

    @Test
    public void testGet() {
        Plugin plugin = pluginDao.get("zorroa-test");
        assertEquals("zorroa-test", plugin.getName());
    }

    @Test(expected= EmptyResultDataAccessException.class)
    public void testGetFailure() {
        pluginDao.get("zorroa-foo");
    }

}
