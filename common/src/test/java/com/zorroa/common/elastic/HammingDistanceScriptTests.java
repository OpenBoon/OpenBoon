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
    public void testHashHeaderResolution() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("field", "foo.hash");
        map.put("hashes", ImmutableList.of("#0060FAAAA"));

        HammingDistanceScript script = new HammingDistanceScript(map);
        assertEquals(15, script.getResolution());

        double score = script.charHashesComparison(new BytesRef("#0060FAAPP".getBytes()));
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
