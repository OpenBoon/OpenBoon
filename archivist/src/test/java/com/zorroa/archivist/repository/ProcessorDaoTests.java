package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.domain.Processor;
import com.zorroa.archivist.domain.ProcessorFilter;
import com.zorroa.sdk.plugins.PluginSpec;
import com.zorroa.sdk.plugins.ProcessorSpec;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.junit.Assert.*;

/**
 * Created by chambers on 8/17/16.
 */
public class ProcessorDaoTests extends AbstractTest {

    @Autowired
    PluginDao pluginDao;

    @Autowired
    ProcessorDao processorDao;

    Plugin plugin;
    PluginSpec spec;

    ProcessorSpec pspec;
    Processor proc;

    @Before
    public void init() {
        spec = new PluginSpec();
        spec.setLanguage("java");
        spec.setDescription("description");
        spec.setName("test");
        spec.setVersion("1.0");
        spec.setPublisher("Zorroa Corp 2016");

        logger.info("plugin: {}", pluginDao);
        plugin = pluginDao.create(spec);

        pspec = new ProcessorSpec();
        pspec.setDescription("foo");
        pspec.setClassName("com.foo.Bar");
        pspec.setDisplay(Lists.newArrayList());
        pspec.setSupportedExtensions(new String[] { "jpg"});
        pspec.setType("unittest");

        proc = processorDao.create(plugin, pspec);
    }

    public void validate(ProcessorSpec spec, Processor pr) {

        assertEquals(spec.getClassName(), pr.getName());
        assertEquals(spec.getDescription(),pr.getDescription());
        assertEquals(spec.getType(), pr.getType());
        assertEquals(spec.getSupportedExtensions().length, pr.getSupportedExtensions().size());
        assertEquals(spec.getDisplay(), pr.getDisplay());

        assertEquals("com.foo", pr.getModule());
        assertEquals("Bar", pr.getShortName());
        assertEquals(plugin.getName(), pr.getPluginName());
        assertEquals(plugin.getVersion(), pr.getPluginVersion());
        assertEquals(plugin.getLanguage(), pr.getPluginLanguage());
    }

    @Test
    public void testGet() {
        Processor pr1 = processorDao.get(proc.getId());
        Processor pr2 = processorDao.get(proc.getName());
        Processor pr3 = processorDao.refresh(proc);
        validate(pspec, pr1);
        validate(pspec, pr2);
        validate(pspec, pr3);
    }

    @Test
    public void testExists() {
        assertTrue(processorDao.exists("com.foo.Bar"));
        assertFalse(processorDao.exists("com.foo.Bing"));
    }

    @Test
    public void testGetAll() {
        assertEquals(1, processorDao.getAll().size());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testDelete() {
        assertTrue(processorDao.delete(proc.getId()));
        processorDao.get(proc.getId());
    }
    @Test
    public void testCount() {
        assertEquals(1, processorDao.count());
    }

    @Test
    public void getAllWithFilter() {

        // name
        ProcessorFilter filter = new ProcessorFilter();
        filter.setNames(Lists.newArrayList("com.foo.Bar"));
        assertEquals(1, processorDao.getAll(filter).size());

        filter = new ProcessorFilter();
        filter.setNames(Lists.newArrayList("com"));
        assertEquals(0, processorDao.getAll(filter).size());

        // module
        filter = new ProcessorFilter();
        filter.setModules(Lists.newArrayList("com.foo"));
        assertEquals(1, processorDao.getAll(filter).size());

        filter = new ProcessorFilter();
        filter.setModules(Lists.newArrayList("com"));
        assertEquals(0, processorDao.getAll(filter).size());

        // type
        filter = new ProcessorFilter();
        filter.setTypes(Lists.newArrayList("unittest"));
        assertEquals(1, processorDao.getAll(filter).size());

        filter = new ProcessorFilter();
        filter.setTypes(Lists.newArrayList("com"));
        assertEquals(0, processorDao.getAll(filter).size());

        // plugin
        filter = new ProcessorFilter();
        filter.setPlugins(Lists.newArrayList(plugin.getId()));
        assertEquals(1, processorDao.getAll(filter).size());

        filter = new ProcessorFilter();
        filter.setPlugins(Lists.newArrayList(-1));
        assertEquals(0, processorDao.getAll(filter).size());
    }


}
