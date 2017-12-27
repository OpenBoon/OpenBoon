package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.domain.Processor;
import com.zorroa.archivist.repository.PipelineDao;
import com.zorroa.archivist.repository.PluginDao;
import com.zorroa.archivist.repository.ProcessorDao;
import com.zorroa.sdk.plugins.PluginSpec;
import com.zorroa.sdk.processor.PipelineType;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.processor.ProcessorSpec;
import com.zorroa.sdk.processor.ProcessorType;
import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by chambers on 6/30/16.
 */
public class PluginServiceTests extends AbstractTest {

    @Autowired
    PluginService pluginService;

    @Autowired
    PluginDao pluginDao;

    @Autowired
    ProcessorDao processorDao;

    @Autowired
    PipelineDao pipelineDao;

    Plugin plugin;
    PluginSpec spec;

    ProcessorSpec pspec;
    Processor proc;

    @Before
    public void init() {
        spec = new PluginSpec();
        spec.setLanguage("java");
        spec.setDescription("a java plugin");
        spec.setName("test");
        spec.setVersion("1.0");
        spec.setPublisher("Zorroa Corp 2016");
        spec.setMd5(Md5Crypt.md5Crypt(UUID.randomUUID().toString().getBytes()));
        plugin = pluginDao.create(spec);

        pspec = new ProcessorSpec();
        pspec.setDescription("foo");
        pspec.setClassName("com.foo.Bar");
        pspec.setDisplay(Lists.newArrayList());
        pspec.setFileTypes(Sets.newHashSet("foo"));
        pspec.setFilters(Lists.newArrayList("_doc.source.extension=='jpg'"));
        pspec.setType(ProcessorType.Import);

        proc = processorDao.create(plugin, pspec);
    }

    @Test
    public void testInstallPlugin() throws IOException {
        try {
            Plugin p = pluginService.getPlugin("zorroa-test");
            pluginService.deletePlugin(p);
        }
        catch (Exception e) {
            // This is ok.
            logger.warn("Failed to delete plugin: ",e);
        }

        int size = pluginService.getAllPlugins().size();

        pluginService.installPlugin(resources.resolve("plugins/zorroa-test-plugin.zip"));
        assertEquals(size+1, pluginService.getAllPlugins().size());
    }

    @Test
    public void getProcessorRef() {
        ProcessorRef ref = pluginService.getProcessorRef("com.foo.Bar");
        assertEquals("java", ref.getLanguage());
        assertEquals("com.foo.Bar", ref.getClassName());
        assertEquals(ImmutableSet.of("foo"), ref.getFileTypes());
    }

    @Test
    public void getProcessorRefs() {
        ProcessorRef ref = pluginService.getProcessorRef("com.foo.Bar");
        ref.getFileTypes().add("spock");
        List<ProcessorRef> refs = pluginService.getProcessorRefs(Lists.newArrayList(ref));
        assertTrue(refs.get(0).getFileTypes().contains("spock"));
        assertTrue(refs.get(0).getFileTypes().contains("foo"));
    }

    @Test
    public void testInstallBundledPipelines() {
        assertFalse(pipelineDao.exists("Import Pipeline"));
        pluginService.installBundledPipelines();
        assertTrue(pipelineDao.exists("Import Pipeline"));
        Pipeline pl = pipelineDao.getStandard(PipelineType.Import);
        assertEquals("Import Pipeline", pl.getName());
    }
}
