package com.zorroa.common.elastic;

import com.google.common.collect.Lists;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.index.fielddata.ScriptDocValues;
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
    private final List<String> charHashes;
    private final List<Float> weights;
    private int length = 0;
    private double minScore = 0;
    private int resolution;
    private final boolean header;
    private final char version;
    private final int dataPos;
    private final int numHashes;
    private final double possibleScore;
    private final double singleScore;

    public HammingDistanceScript(Map<String, Object> params) {
        super();
        field = (String) params.get("field");
        minScore = (int) params.getOrDefault("minScore", 1) / NORM;
        resolution = 15;

        List<String> _hashes = (List<String>) params.get("hashes");
        List<Float> _weights = (List<Float>) params.get("weights");

        if (_hashes == null) {
            throw new IllegalArgumentException(
                    "Hashes cannot be null");
        }

        if (_weights == null) {
            _weights = Collections.nCopies(_hashes.size(), 1.0f);
        }

        if (_hashes.size() != _weights.size()) {
            throw new IllegalArgumentException(
                    "HammingDistanceScript weights must align with hashes");
        }

        /**
         * Go through all the values and remove the null
         * values and populate the charHashes and
         * weights fields with valid values.
         */
        charHashes = Lists.newArrayList();
        weights = Lists.newArrayList();
        for (int i=0; i<_hashes.size(); i++) {
            String hash = _hashes.get(i);
            if (hash == null || hash.isEmpty()) {
                continue;
            }
            charHashes.add(hash);
            weights.add(_weights.get(i));
        }

        /**
         * If there are no valid hashes left, initialize to defaults
         */
        if (charHashes.isEmpty()) {
            singleScore = possibleScore = numHashes = dataPos = version = 0;
            header = false;
            return;
        }
        else {
            /**
             * Use the first hash to determine if there is a header.
             */
            String hash = charHashes.get(0);
            header = hash.charAt(0) == '#';
            length = hash.length();
            numHashes = charHashes.size();

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
            } else {
                version = 0;
                dataPos = 0;
            }

            // To get the proper score, we subtract header size from the length here.
            singleScore = resolution * (length - dataPos);
            possibleScore = singleScore * numHashes;
        }
    }

    /**
     * Returned if it is impossible to calculate a score
     */
    private static final double NO_SCORE = 0;


    @Override
    public double runAsDouble() {
        ScriptDocValues.Strings strings;

        if (doc().containsKey(field)) {
            strings = docFieldStrings(field);
        }
        else {
            return NO_SCORE;
        }

        double score = charHashesComparison(strings.getBytesValue());
        return score >= minScore ? score : NO_SCORE;
    }

    public final double charHashesComparison(BytesRef fieldValue) {
        double score = 0;
        if (possibleScore == 0) {
            return NO_SCORE;
        }

        if (fieldValue == null || fieldValue.length == 0) {
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
        score = normalize(score);
        return score;
    }

    public final double normalize(double score) {
        score = (score / possibleScore);
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
