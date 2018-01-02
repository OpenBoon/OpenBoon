package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.domain.Processor;
import com.zorroa.archivist.domain.ProcessorFilter;
import com.zorroa.sdk.plugins.PluginSpec;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.plugins.ProcessorSpec;
import com.zorroa.sdk.processor.ProcessorType;
import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.UUID;

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
        spec.setMd5(Md5Crypt.md5Crypt(UUID.randomUUID().toString().getBytes()));
        plugin = pluginDao.create(spec);

        pspec = new ProcessorSpec();
        pspec.setDescription("foo");
        pspec.setClassName("com.foo.Bar");
        pspec.setDisplay(Lists.newArrayList());
        pspec.setFilters(ImmutableList.of("_doc.source.extension=='jpg'"));
        pspec.setType(ProcessorType.Import);

        proc = processorDao.create(plugin, pspec);
    }


    public void validate(ProcessorSpec spec, Processor pr) {

        assertEquals(spec.getClassName(), pr.getName());
        assertEquals(spec.getDescription(), pr.getDescription());
        assertEquals(spec.getType(), pr.getType());
        assertEquals(spec.getFilters().size(), pr.getFilters().size());
        assertEquals(spec.getDisplay(), pr.getDisplay());

        assertEquals("com.foo", pr.getModule());
        assertEquals("Bar", pr.getShortName());
        assertEquals(plugin.getName(), pr.getPluginName());
        assertEquals(plugin.getVersion(), pr.getPluginVersion());
        assertEquals(plugin.getLanguage(), pr.getPluginLanguage());
    }

    @Test
    public void testGetRef() {
        ProcessorRef ref = processorDao.getRef("com.foo.Bar");
        assertEquals("java", ref.getLanguage());
        assertEquals("com.foo.Bar", ref.getClassName());
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
        assertTrue(processorDao.getAll().size() > 0);
    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void testDelete() {
        assertTrue(processorDao.delete(proc.getId()));
        processorDao.get(proc.getId());
    }

    @Test
    public void testCount() {
        assertTrue(processorDao.count() > 0);
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

        /*
        // type
        filter = new ProcessorFilter();
        filter.setTypes(Lists.newArrayList("unittest"));
        assertEquals(1, processorDao.getAll(filter).size());

        filter = new ProcessorFilter();
        filter.setTypes(Lists.newArrayList("com"));
        assertEquals(0, processorDao.getAll(filter).size());
*/
        // plugin
        filter = new ProcessorFilter();
        filter.setPlugins(Lists.newArrayList(plugin.getId()));
        assertEquals(1, processorDao.getAll(filter).size());

        filter = new ProcessorFilter();
        filter.setPlugins(Lists.newArrayList(-1));
        assertEquals(0, processorDao.getAll(filter).size());
    }

    @Test
    public void getAllWithSortingFilter() {

        ProcessorFilter filter = new ProcessorFilter();
        filter.setSort(ImmutableMap.of("pluginName", "asc"));
        assertTrue(processorDao.getAll(filter).size() > 0);

        filter = new ProcessorFilter();
        filter.setSort(ImmutableMap.of("id", "asc", "name", "asc"));
        assertTrue(processorDao.getAll(filter).size() > 0);

        filter = new ProcessorFilter();
        filter.setSort(ImmutableMap.of("shortName", "asc"));
        assertTrue(processorDao.getAll(filter).size() > 0);

        filter = new ProcessorFilter();
        filter.setSort(ImmutableMap.of("module", "asc"));
        assertTrue(processorDao.getAll(filter).size() > 0);

        filter = new ProcessorFilter();
        filter.setSort(ImmutableMap.of("description", "asc"));
        assertTrue(processorDao.getAll(filter).size() > 0);

        filter = new ProcessorFilter();
        filter.setSort(ImmutableMap.of("pluginId", "asc"));
        assertTrue(processorDao.getAll(filter).size() > 0);

        filter = new ProcessorFilter();
        filter.setSort(ImmutableMap.of("pluginLanguage", "asc"));
        assertTrue(processorDao.getAll(filter).size() > 0);
    }
}
