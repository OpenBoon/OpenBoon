package com.zorroa.common.elastic;

import com.zorroa.common.AbstractTest;
import org.junit.Test;

import static com.zorroa.common.elastic.HammingDistanceScript.hexToDecimalArray;
import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 3/1/17.
 */
public class HammingDistanceScriptTests extends AbstractTest {

    @Test
    public void testBitwiseHamming() {
        assertEquals(0, HammingDistanceScript.bitwiseHammingDistance(
                new int[] { 255 }, new int[] { 0 }));
        assertEquals(8, HammingDistanceScript.bitwiseHammingDistance(
                new int[] { 255 }, new int[] { 255 }));

        assertEquals(7, HammingDistanceScript.bitwiseHammingDistance(
                hexToDecimalArray("AF"), hexToDecimalArray("AE")));
        assertEquals(14, HammingDistanceScript.bitwiseHammingDistance(
                hexToDecimalArray("AFCC"), hexToDecimalArray("AECD")));
    }

    @Test
    public void testHamming() {
        assertEquals(29,
                HammingDistanceScript.hammingDistance("AB", "AC", 2));
        assertEquals(112,
                HammingDistanceScript.hammingDistance("AFAFAFAF", "ADADADAD", 8));
    }

    @Test
    public void testHammingWeight() {
        assertEquals(8, HammingDistanceScript.hammingWeight(255));
        assertEquals(1, HammingDistanceScript.hammingWeight(1));
        assertEquals(2, HammingDistanceScript.hammingWeight(3));
        assertEquals(3, HammingDistanceScript.hammingWeight(7));
        assertEquals(1, HammingDistanceScript.hammingWeight(8));
    }

}
