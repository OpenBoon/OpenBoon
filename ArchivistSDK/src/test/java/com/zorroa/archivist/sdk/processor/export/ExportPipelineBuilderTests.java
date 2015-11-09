package com.zorroa.archivist.sdk.processor.export;

import com.zorroa.archivist.sdk.domain.ExportPipelineBuilder;
import com.zorroa.archivist.sdk.domain.ConnectionBuilder;
import com.zorroa.archivist.sdk.processor.ExportProcessorFactory;
import com.zorroa.archivist.sdk.util.Json;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the serialization and deserialization of the ExportProcessorFactory
 */
public class ExportPipelineBuilderTests {

    /**
     * Test we can serialize/deserialize an ExportPipeline builder.
     */
    @Test
    public void serializeAndDeserialize() {

        ExportPipelineBuilder builder1 = new ExportPipelineBuilder();
        builder1.setName("standard");

        ExportProcessorFactory factory1 = new ExportProcessorFactory();
        factory1.setKlass("com.zorroa.archivist.sdk.processor.export.TestExportProcessor");
        factory1.setName("TestExportProcessor01");
        factory1.setArg("stringInput", "foo");
        factory1.setArg("intInput", 1);
        factory1.setArg("floatInput", 1.5);

        ExportProcessorFactory factory2 = new ExportProcessorFactory();
        factory2.setKlass("com.zorroa.archivist.sdk.processor.export.TestExportProcessor");
        factory2.setName("TestExportProcessor02");
        factory2.setArg("stringInput", "foo");
        factory2.setArg("intInput", 1);
        factory2.setArg("floatInput", 1.5);

        builder1.addToConnections("TestExportProcessor01::stringOutput", "TestExportProcessor02::stringInput");

        builder1.addToProcessors(factory1);
        builder1.addToProcessors(factory2);

        /*
         * Serialize the builder1, then rehydrate the bytes into builder2.  Once that is done
         * check to see if the values came through properly.
         */
        ExportPipelineBuilder builder2 = Json.deserialize(Json.serialize(builder1), ExportPipelineBuilder.class);

        assertEquals(builder1.getName(), builder2.getName());
        assertEquals(builder1.getConnections().size(), builder2.getConnections().size());
        assertEquals(builder1.getProcessors().size(), builder2.getProcessors().size());

        /*
         * Validate the connection is intact
         */
        ConnectionBuilder conn1 = builder1.getConnections().get(0);
        ConnectionBuilder conn2 = builder2.getConnections().get(0);


        assertEquals(conn1.getCord()[0], conn2.getCord()[0]);
        assertEquals(conn1.getCord()[1], conn2.getCord()[1]);

        assertEquals(conn1.getSocket()[0], conn2.getSocket()[0]);
        assertEquals(conn1.getSocket()[1], conn2.getSocket()[1]);

        /*
         * Validate the processors
         */
        ExportProcessorFactory factory1_b1 = builder1.getProcessors().get(0);
        ExportProcessorFactory factory1_b2 = builder2.getProcessors().get(0);

        assertEquals(factory1_b1.getName(), factory1_b2.getName());
        assertEquals(factory1_b1.getKlass(), factory1_b2.getKlass());
        assertEquals(factory1_b1.getArgs(), factory1_b2.getArgs());

    }

}
