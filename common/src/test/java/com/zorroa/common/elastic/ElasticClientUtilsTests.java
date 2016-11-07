package com.zorroa.common.elastic;

import com.zorroa.common.AbstractTest;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.processor.Source;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 11/7/16.
 */
public class ElasticClientUtilsTests extends AbstractTest {

    Asset asset1;

    @Autowired
    AssetDao assetDao;

    @Before
    public void init() {
        Source builder = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        asset1 = assetDao.index(builder, null);
        refreshIndex();
    }

    @Test
    public void testGetFieldType() {
        assertEquals("string", ElasticClientUtils.getFieldType(client, "archivist", "asset", "source.basename"));
        assertEquals("long", ElasticClientUtils.getFieldType(client, "archivist", "asset", "source.fileSize"));
    }
}
