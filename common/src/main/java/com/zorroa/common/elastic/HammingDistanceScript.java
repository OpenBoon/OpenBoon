package com.zorroa.common.elastic;

import com.amazonaws.util.CollectionUtils;
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
    private List<String> charHashes;
    private List<Float> weights;
    private int length = 0;
    private int numHashes = 0;
    private int resolution = 0;
    private int minScore = 0;
    private boolean header = false;
    private char version = 0;
    private int headerSize = 0;
    private final double possibleScore;

    public HammingDistanceScript(Map<String, Object> params) {
        super();
        field = (String) params.get("field");
        header = (field.endsWith( ".hash") || field.startsWith("similarity.")) && !field.endsWith("byte");
        weights = (List<Float>) params.get("weights");
        minScore = (int) params.getOrDefault("minScore", 1);
        resolution = 15;

        if (!header && !field.endsWith(".raw")) {
            field = field.concat(".raw");
        }

        charHashes = (List<String>) params.get("hashes");
        if (!CollectionUtils.isNullOrEmpty(charHashes)) {
            String hash = charHashes.get(0);
            numHashes = charHashes.size();
            length = hash.length();

            /**
             * TODO: more sophisticated header parsing.
             *
             * There are 2 fields every has leads with:
             * 1 char: version
             * 2 chars: position of data (called "headerSize" here)
             *
             * A version 0 hash has 1 field, resolution.
             */
            if (header) {
                version = hash.charAt(0);

                // +4 on the headerSize to take into account the header size byte.
                headerSize = Integer.parseInt(hash.substring(1, 3), 16) + 3;

                if (version <= 0) {
                    // Resolution is the next byte.
                    resolution = Integer.parseInt(hash.substring(3, 5), 16);
                }
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

        // To get the proper score, we subtract header size from the length here.
        possibleScore = resolution * (length - headerSize) * numHashes;
    }

    @Override
    public double runAsDouble() {
        if (possibleScore == 0) {
            return 0;
        }

        double score = charHashesComparison(
                    docFieldStrings(field).getBytesValue());
        return score >= minScore ? score : 0;
    }

    public final double charHashesComparison(BytesRef fieldValue) {
        long score = 0;

        byte ver = fieldValue.bytes[0];
        for (int i = 0; i < numHashes; ++i) {
            String hash = charHashes.get(i);
            if (ver != hash.charAt(0)) {
                continue;
            }
            score += weights.get(i) * hammingDistance(fieldValue, hash);
        }

        return normalize(score);
    }

    public final double normalize(double score) {
        score = (possibleScore - score) * NORM / possibleScore;
        return score;
    }

    public final long hammingDistance(final BytesRef lhs, final String rhs) {
        long score = 0;
        for (int i = headerSize; i < length; i++) {
            score += Math.abs(lhs.bytes[i] - rhs.charAt(i));
        }
        return score;
    }

    public int getResolution() {
        return resolution;
    }
}
