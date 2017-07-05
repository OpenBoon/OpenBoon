package com.zorroa.common.elastic;

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.script.AbstractDoubleSearchScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private int dataPos = 0;
    private final double possibleScore;
    private final double singleScore;

    public HammingDistanceScript(Map<String, Object> params) {
        super();
        field = (String) params.get("field");
        // Nothing should be raw.
        if (field.endsWith(".raw")) {
            field = field.replaceAll("\\.raw$", "");
        }
        weights = (List<Float>) params.get("weights");
        minScore = (int) params.getOrDefault("minScore", 1);
        resolution = 15;

        charHashes = (List<String>) params.get("hashes");

        if (charHashes != null) {
            // filter out null entries, then check for emptiness
            charHashes = charHashes.stream().filter(s->s!= null).collect(Collectors.toList());
            numHashes = charHashes.size();

            if (!charHashes.isEmpty()) {
                String hash = charHashes.get(0);
                header = hash.charAt(0) == '#';
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
                    version = hash.charAt(1);

                    // The start position of the data.
                    dataPos = Integer.parseInt(hash.substring(2, 4), 16);

                    if (version <= 0) {
                        // Resolution is the next byte.
                        resolution = Integer.parseInt(hash.substring(4, 6), 16);
                    }
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
        singleScore = resolution * (length - dataPos);
        possibleScore = singleScore * numHashes;
    }

    @Override
    public double runAsDouble() {
        double score = charHashesComparison(
                    docFieldStrings(field).getBytesValue());
        return score >= minScore ? score : 0;
    }

    /**
     * Returned if it is impossible to calculate a score.
     */
    private static final double NO_SCORE = 0;

    public final double charHashesComparison(BytesRef fieldValue) {
        double score = 0;

        if (possibleScore == 0) {
            return NO_SCORE;
        }

        if (fieldValue == null) {
            return NO_SCORE;
        }
        if (fieldValue.bytes == null) {
            return NO_SCORE;
        }

        byte ver = fieldValue.bytes[1];
        for (int i = 0; i < numHashes; ++i) {
            String hash = charHashes.get(i);
            if (hash == null) {
                continue;
            }
            if (header) {
                if (ver != hash.charAt(1)) {
                    continue;
                }
            }
            else {
                if (fieldValue.length != hash.length()) {
                    continue;
                }
            }
            score += (weights.get(i) * hammingDistance(fieldValue, hash));
        }
        return normalize(score);
    }

    public final double normalize(double score) {
        score = (score / possibleScore) * NORM;
        return score;
    }

    public final double hammingDistance(final BytesRef lhs, final String rhs) {
        double score = singleScore;
        for (int i = dataPos; i < length; i++) {
            score -= Math.abs(lhs.bytes[i] - rhs.charAt(i));
        }
        return score;
    }

    public int getResolution() {
        return resolution;
    }
    public String getField() { return field; }
    public int getNumHashes() {
        return numHashes;
    }
}
