package com.zorroa.archivist;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AssetBuilderTests extends AbstractTest {

    @Test
    public void testCreate() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assertEquals("jpg", builder.getExtension());
        assertEquals(getStaticImagePath(), builder.getDirectory());
        assertEquals("beer_kettle_01.jpg", builder.getFilename());
        assertEquals(getStaticImagePath() + "/beer_kettle_01.jpg", builder.getAbsolutePath());
    }
}
