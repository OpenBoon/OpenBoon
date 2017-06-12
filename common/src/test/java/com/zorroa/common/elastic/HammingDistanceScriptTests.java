package com.zorroa.common.elastic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.zorroa.common.AbstractTest;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 3/1/17.
 */
public class HammingDistanceScriptTests extends AbstractTest {

    @Test
    public void testCharHammingIdenticalMatch() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("field", "charHash");
        map.put("hashes", ImmutableList.of("AAAA"));

        HammingDistanceScript script = new HammingDistanceScript(map);
        double score = script.charHashesComparison(new BytesRef("AAAA".getBytes()));
        assertEquals(100, score, 0);
    }

    @Test
    public void testCharHammingHalfMatch() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("field", "charHash");
        map.put("hashes", ImmutableList.of("AAAA"));

        HammingDistanceScript script = new HammingDistanceScript(map);
        double score = script.charHashesComparison(new BytesRef("AAPP".getBytes()));
        assertEquals(50, score, 0);
    }

    @Test
    public void testNumericHammingIdenticalMatch() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("field", "hash._byte");
        map.put("hashes", ImmutableList.of(ImmutableList.of(1,1,1,1)));

        HammingDistanceScript script = new HammingDistanceScript(map);
        double score = script.numericHashesComparison(new LDocValues(1,1,1,1));
        assertEquals(100, score, 0);
    }

    @Test
    public void testNumericHammingRoughHalfMatch() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("field", "hash._byte");
        map.put("hashes", ImmutableList.of(ImmutableList.of(127, -128, 127, -128)));

        HammingDistanceScript script = new HammingDistanceScript(map);
        double score = script.numericHashesComparison(new LDocValues(0,0,0,0));
        assertEquals(50, score, 0);
    }

    /**
     * An impl of a wrapper class ElasticSearch uses.  We use this in
     * the plugin to avoid copying values
     */
    public static class LDocValues extends SortedNumericDocValues {
        Number[] values;
        public LDocValues(Number ... values) {
            this.values = values;
        }
        @Override
        public void setDocument(int i) {

        }
        @Override
        public long valueAt(int i) {
            return values[i].longValue();
        }
        @Override
        public int count() {
            return values.length;
        }
    }

}
