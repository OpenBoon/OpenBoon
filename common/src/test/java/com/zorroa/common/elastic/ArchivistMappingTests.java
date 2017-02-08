package com.zorroa.common.elastic;

import com.google.common.collect.ImmutableMap;
import com.zorroa.common.AbstractTest;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 2/8/17.
 */
public class ArchivistMappingTests extends AbstractTest {

    /**
     * Test to ensure the mapping handles geo points.
     *
     * @throws IOException
     */
    @Test
    public void testGeoPoint() throws IOException {
        client.prepareIndex("archivist", "asset")
                .setSource(ImmutableMap.of("location", ImmutableMap.of("point",
                        new double[] { 1.23, 2.32} )))
                .get();
        refreshIndex();

        ClusterState cs = client.admin().cluster().prepareState().setIndices("archivist").execute().actionGet().getState();
        IndexMetaData imd = cs.getMetaData().index("archivist");
        MappingMetaData mdd = imd.mapping("asset");
        assertEquals("geo_point", getMappingType(mdd.getSourceAsMap(), "location.point"));
    }

}
