package com.zorroa.common.elastic;

import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.script.AbstractFloatSearchScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 12/6/16.
 */
public class HammingDistanceScript extends AbstractFloatSearchScript {

    private static final Logger logger = LoggerFactory.getLogger(HammingDistanceScript.class);

    private final String field;
    private final int[][] bitwiseHashes;
    private final List<String> hashes;
    private final boolean bitwise;
    private final int length;
    private final int minScore;

    public HammingDistanceScript(Map<String, Object> params) {
        super();
        field = forceRaw((String) params.get("field"));
        bitwise = field.endsWith( ".bit.raw");
        hashes = (List<String>) params.get("hashes");
        length = hashes.get(0).length();
        minScore = (int) params.getOrDefault("minScore", 1);

        if (hashes == null || hashes.isEmpty()) {
            bitwiseHashes = null;
            return;
        }

        /**
         * Initialize for bitwise mode which pre-caches the passed in hex
         * as arrays of integers.
         */
        if (bitwise) {
            this.bitwiseHashes = new int[hashes.size()][length / 2];
            for (int i=0; i<hashes.size(); i++) {
                String hash = hashes.get(i);
                if (hash.length() != length) {
                    throw new IllegalArgumentException(
                            "HammingDistanceScript hashes must all be of the same length");
                }
                // pre-cache this so we don't do the conversion on every hit.
                this.bitwiseHashes[i] = hexToDecimalArray(hash);
            }
        }
        else {
            bitwiseHashes = null;
        }
    }

    @Override
    public float runAsFloat() {
        String fieldValue = ((ScriptDocValues.Strings) doc().get(field)).getValue();
        if(fieldValue == null || length == 0) {
            return 0.0f;
        }

        int distance = 0;

        if (bitwise) {
            final int[] fieldValueBytes = hexToDecimalArray(fieldValue);
            if (fieldValueBytes == null) {
                return 0.0f;
            }

            for (int[] hash: bitwiseHashes) {
                if (fieldValueBytes.length != hash.length) {
                    continue;
                }
                distance += bitwiseHammingDistance(fieldValueBytes, hash);
            }
            // Normalize the returned distance to 0-100.0
            distance *= 100.0f / (4 * length * hashes.size());
        }
        else {
            final int fieldLength = fieldValue.length();
            for (String hash : hashes) {
                if (fieldLength != length) {
                    continue;
                }
                distance += hammingDistance(fieldValue, hash, length);
            }
            // Normalize the returned distance to 0-100.0
            distance *= 100.0f / (15 * length * hashes.size());
        }

        /*
         * If distance >= minScore then return minScore, otherwise
         * return 0.
         */
        return distance >= minScore ? distance : 0;
    }

    public final static int hammingDistance(final String lhs, final String rhs, int length) {
        int distance = 15 * length;
        for (int i = 0, l = length; i < l; i++) {
            distance -= Math.abs(lhs.charAt(i) - rhs.charAt(i));
        }
        return distance;
    }

    public final static int bitwiseHammingDistance(final int[] lhs, final int[] rhs) {
        int distance = 0;
        for (int i = 0, l = lhs.length; i < l; i++) {
            int z = lhs[i] ^ rhs[i];
            //while (z > 0) {
            //    distance += 1;
            //    z &= z-1;
            //}
            distance +=hammingWeight(z);
        }
        return (lhs.length * 8) - distance;
    }

    public final static int hammingWeight(int i) {
        i = i - ((i >>> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
        return (((i + (i >>> 4)) & 0x0F0F0F0F) * 0x01010101) >>> 24;
    }

    public final static int[] hexToDecimalArray(final String value) {
        int[] result = new int[value.length() / 2];
        for (int i = 0, l = value.length(); i < l; i=i+2) {
            try {
                result[i / 2] = Integer.parseInt(value.substring(i, i + 2), 16);
            } catch (IndexOutOfBoundsException e) {
                logger.warn("Failed to calculate hex array, {} - odd length {}", value, value.length());
                return null;
            }
        }
        return result;
    }

    public String forceRaw(String field) {
        if (!field.endsWith(".raw")) {
            field = field + ".raw";
        }
        return field;
    }
}
