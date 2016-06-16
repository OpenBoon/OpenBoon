package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystBuilder;
import com.zorroa.sdk.processor.DisplayProperties;
import com.zorroa.sdk.processor.ProcessorProperties;
import com.zorroa.sdk.processor.ProcessorType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 5/20/16.
 */
public class AnalystServiceTests extends AbstractTest {

    AnalystBuilder ping;

    @Before
    public void init() {
        ping = sendAnalystPing();
    }

    @Test
    public void testGet() {
        Analyst a = analystService.get(ping.getUrl());
        assertEquals(ping.getUrl(), a.getUrl());
    }

    @Test
    public void testGetProcessors() {
        assertEquals(3, analystService.getProcessors().size());
    }

    @Test
    public void testGetProcessorsByType() {
        for (ProcessorType type: ProcessorType.values()) {
            assertEquals(type, analystService.getProcessors(type).get(0).getType());
        }
    }

    @Test
    public void testValidateProcessorDescription() {
        ProcessorProperties p = analystService.getProcessors(ProcessorType.Ingest).get(0);
        assertEquals("foo.bar.FooIngestor", p.getClassName());
        assertEquals(ProcessorType.Ingest, p.getType());
        for (DisplayProperties d: p.getDisplay()) {
            assertEquals("field", d.getName());
            assertEquals("text", d.getWidget());
        }
    }
}
