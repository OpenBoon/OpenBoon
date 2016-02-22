package com.zorroa.analyst.ingestors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by wex on 2/20/16.
 */
public final class ModelUtils {
    private static final Logger logger = LoggerFactory.getLogger(ModelUtils.class);

    private ModelUtils() {}     // Disallow instantiation
    
    public static String modelPath() {
        Map<String, String> env = System.getenv();
        String modelPath = env.get("ZORROA_MODEL_PATH");
        if (modelPath == null) {
            logger.error("ModelUtils requires ZORROA_MODEL_PATH");
            return null;
        }
        return modelPath;
    }
}
