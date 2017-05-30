package com.zorroa.common.elastic;

import com.zorroa.common.AbstractTest;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 11/7/16.
 */
public class ElasticClientUtilsTests extends AbstractTest {

    @Before
    public void init() {
        Source builder = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        client.prepareIndex("archivist", "asset", builder.getId()).setSource(Json.serialize(builder.getDocument()))
                .setRefresh(true)
                .get();
    }

    @Test
    public void testGetFieldType() {
        assertEquals("string", ElasticClientUtils.getFieldType(client, "archivist", "asset", "source.basename"));
        assertEquals("long", ElasticClientUtils.getFieldType(client, "archivist", "asset", "source.fileSize"));
    }
}
