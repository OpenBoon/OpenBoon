package com.zorroa.common.elastic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zorroa.common.AbstractTest;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.util.Json;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.termvectors.TermVectorsResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    /**
     * Test to ensure the mapping handles geo points.
     *
     * @throws IOException
     */
    @Test
    public void testPathMapping() throws IOException, ExecutionException, InterruptedException {
        IndexResponse rsp = client.prepareIndex("archivist", "asset")
                .setSource(ImmutableMap.of("path", "/foo/bar/bing"))
                .get();
        refreshIndex();

        ClusterState cs = client.admin().cluster().prepareState().setIndices("archivist").execute().actionGet().getState();
        IndexMetaData imd = cs.getMetaData().index("archivist");
        MappingMetaData mdd = imd.mapping("asset");
        assertEquals("string", getMappingType(mdd.getSourceAsMap(), "path"));

        TermVectorsResponse tv = client.prepareTermVectors()
                .setIndex("archivist")
                .setType("asset")
                .setId(rsp.getId())
                .setSelectedFields("path")
                .setOffsets(true)
                .setPositions(true)
                .setTermStatistics(true)
                .setFieldStatistics(true)
                .execute().get();
        refreshIndex();
        assertEquals(ImmutableList.of("/foo", "/foo/bar", "/foo/bar/bing"),  getTerms(tv));
    }


    @Test
    public void testPathMappingRegexMatcher() {

        Source source = new Source(getTestImagePath("set01/standard/faces.jpg"));
        source.setAttr("test.directory", "/foo/bar");
        source.setAttr("test.fooPath", "/foo/bar");

        client.prepareIndex("archivist", "asset", source.getId())
                .setSource(Json.serialize(source.getDocument()))
                .setRefresh(true)
                .get();

        Document doc = new Document(ElasticClientUtils.getMapping(client, "archivist", "asset"));
        assertEquals("path_analyzer", doc.getAttr("properties.test.properties.directory.analyzer"));
        assertEquals("path_analyzer", doc.getAttr("properties.test.properties.fooPath.analyzer"));

    }

    public List<String> getTerms(TermVectorsResponse resp) throws IOException {
        List<String> termStrings = new ArrayList<>();
        Fields fields = resp.getFields();
        Iterator<String> iterator = fields.iterator();
        while (iterator.hasNext()) {
            String field = iterator.next();
            Terms terms = fields.terms(field);
            TermsEnum termsEnum = terms.iterator();
            while(termsEnum.next() != null){
                BytesRef term = termsEnum.term();
                if (term != null) {
                    termStrings.add(term.utf8ToString());
                }
            }
        }
        return termStrings;
    }
}
