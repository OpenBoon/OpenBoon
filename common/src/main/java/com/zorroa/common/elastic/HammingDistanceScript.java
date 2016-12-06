package com.zorroa.common.elastic;

import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.script.AbstractFloatSearchScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by chambers on 12/6/16.
 */
public class HammingDistanceScript extends AbstractFloatSearchScript {

    private static final Logger logger = LoggerFactory.getLogger(HammingDistanceScript.class);

    private final String field;
    private final String hash;
    private final int length;

    public HammingDistanceScript(Map<String, Object> params) {
        super();
        field = (String) params.get("field");
        hash = (String) params.get("hash");
        length = hash == null ? 0: hash.length();
    }

    private int hammingDistance(String lhs, String rhs) {
        int distance = length;
        for (int i = 0, l = lhs.length(); i < l; i++) {
            if (lhs.charAt(i) != rhs.charAt(i)) {
                distance--;
            }
        }

        return distance;
    }

    @Override
    public float runAsFloat() {
        String fieldValue = ((ScriptDocValues.Strings) doc().get(field)).getValue();
        if(hash == null || fieldValue == null || fieldValue.length() != hash.length()){
            return 0.0f;
        }

        int distance =  hammingDistance(fieldValue, hash);
        return distance;
    }
}
