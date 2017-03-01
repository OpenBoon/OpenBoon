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
    private final List<String> hashes;
    private final int length;

    public HammingDistanceScript(Map<String, Object> params) {
        super();
        field = (String) params.get("field");
        hashes = (List<String>) params.get("hashes");
        if (hashes == null || hashes.size() == 0) {
            length = 0;
        }
        else {
            /**
             * Sample the length just 1 time.  All of the hashes should
             * have the same length.
             */
            length = hashes.get(0).length();
            for (String hash: hashes) {
                if (hash.length() != length) {
                    throw new IllegalArgumentException(
                            "HammingDistanceScript hashes must all be of the same length");
                }
            }
        }
    }

    private int hammingDistance(String lhs, String rhs) {
        int distance = length;
        for (int i = 0, l = length; i < l; i++) {
            if (lhs.charAt(i) != rhs.charAt(i)) {
                distance--;
            }
        }

        return distance;
    }

    @Override
    public float runAsFloat() {
        String fieldValue = ((ScriptDocValues.Strings) doc().get(field)).getValue();
        if(fieldValue == null || length == 0) {
            return 0.0f;
        }

        int distance = 0;
        for (String hash: hashes) {
            if (fieldValue.length() != length) {
                continue;
            }
            distance += hammingDistance(fieldValue, hash);
        }
        return distance;
    }
}
