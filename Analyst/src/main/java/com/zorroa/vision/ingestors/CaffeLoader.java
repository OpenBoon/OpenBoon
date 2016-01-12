/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.vision.ingestors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaffeLoader {
    private static final Logger logger = LoggerFactory.getLogger(CaffeIngestor.class);

    static {
        // Note: Must use java -Djava.library.path=<path-to-jnilib>
        logger.info("CaffeLoader initializing CaffeClassifier JNI...");
        try {
            System.loadLibrary("CaffeClassifier");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("CaffeClassifier native code library failed to load.\n" + e);
        }
        logger.info("CaffeLoader finished loading.");
    }
}
