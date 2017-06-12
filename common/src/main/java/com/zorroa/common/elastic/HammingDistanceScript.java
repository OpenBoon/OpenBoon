package com.zorroa.common.elastic;

import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.script.AbstractDoubleSearchScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 12/6/16.
 */
public final class HammingDistanceScript extends AbstractDoubleSearchScript {

    private static final Logger logger = LoggerFactory.getLogger(HammingDistanceScript.class);

    private static final double NORM = 100.0;

    private String field;
    private List<List<Integer>> intHashes;
    private List<String> charHashes;
    private List<Float> weights;
    private int length;
    private int numHashes;
    private int resolution;
    private final int minScore;
    private final boolean numeric;
    private final double possibleScore;

    public HammingDistanceScript(Map<String, Object> params) {
        super();

        field = (String) params.get("field");
        numeric = field.endsWith( "._byte");
        weights = (List<Float>) params.get("weights");
        minScore = (int) params.getOrDefault("minScore", 1);

        if (numeric) {
            intHashes = (List<List<Integer>>) params.get("hashes");
            resolution = 255;
            if (intHashes == null || intHashes.isEmpty()) {
                numHashes = 0;
                length = 0;
            }
            else {
                numHashes = intHashes.size();
                length = intHashes.get(0).size();
            }
        }
        else {
            if (!field.endsWith(".raw")) {
                field = field.concat(".raw");
            }
            resolution = 15;
            charHashes = (List<String>) params.get("hashes");
            if (charHashes == null || charHashes.isEmpty()) {
                numHashes = 0;
                length = 0;
            }
            else {
                numHashes = charHashes.size();
                length = charHashes.get(0).length();
            }
        }

        if (weights == null) {
            weights = Collections.nCopies(numHashes, 1.0f);
        }

        // Need 1 weight per hash.
        if (weights.size() != numHashes) {
            throw new IllegalArgumentException(
                    "HammingDistanceScript weights must align with hashes");
        }

        possibleScore = resolution * length * numHashes;
    }

    @Override
    public double runAsDouble() {
        if (possibleScore == 0) {
            return 0;
        }

        double score;
        if (numeric)  {
            score = numericHashesComparison(
                    docFieldLongs(field).getInternalValues());
        }
        else {
            score = charHashesComparison(
                    docFieldStrings(field).getBytesValue());
        }
        return score >= minScore ? score : 0;
    }

    public final double charHashesComparison(BytesRef fieldValue) {
        long score = 0;

        final int fieldLength = fieldValue.length;
        for (int i = 0; i < numHashes; ++i) {
            if (fieldLength != length) {
                continue;
            }
            String hash = charHashes.get(i);
            score += weights.get(i) * hammingDistance(fieldValue, hash);
        }

        return normalize(score);
    }

    public final double numericHashesComparison(SortedNumericDocValues fieldValue) {
        long score = 0;

        for (int i = 0; i < numHashes; ++i) {
            if (fieldValue.count() != length) {
                continue;
            }

            List<Integer> hash = intHashes.get(i);
            score+= weights.get(i) * hammingDistance(fieldValue, hash);
        }

        return normalize(score);
    }

    public final double normalize(double score) {
        score = (possibleScore - score) * NORM / possibleScore;
        return score;
    }

    public final long hammingDistance(final SortedNumericDocValues lhs, final List<Integer> rhs) {
        long score = 0;
        for (int i=0; i < length; i++) {
            score += Math.abs(lhs.valueAt(i) - rhs.get(i));
        }
        return score;
    }

    public final long hammingDistance(final BytesRef lhs, final String rhs) {
        long score = 0;
        for (int i = 0; i < length; i++) {
            score += Math.abs(lhs.bytes[i] - rhs.charAt(i));
        }
        return score;
    }
}
