package com.zorroa.archivist;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.zorroa.archivist.domain.AssetBuilder;

public class AssetBuilderTests extends ArchivistApplicationTests {

    @Test
    public void testCreate() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assertEquals("jpg", builder.getExtension());
        assertEquals(getStaticImagePath(), builder.getDirectory());
        assertEquals("beer_kettle_01.jpg", builder.getFilename());
        assertEquals(getStaticImagePath() + "/beer_kettle_01.jpg", builder.getAbsolutePath());
    }
}
