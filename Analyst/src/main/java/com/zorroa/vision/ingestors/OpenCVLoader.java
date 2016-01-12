/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.vision.ingestors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statically loads the shared OpenCV java classes to avoid having
 * link collisions when separate plugins load the same dylib.
 * Requires a shared ProcessorFactory<> classLoader to avoid
 * re-loading the static OpenCVLoader libraries.
 */
public class OpenCVLoader {
    private static final Logger logger = LoggerFactory.getLogger(CaffeIngestor.class);

    static {
	logger.info("OpenCVLoader initializing OpenCV JNI...");
        try {
            System.loadLibrary("opencv_java2412");
            System.out.println("Loaded OpenCV 2412 native library");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("OpenCV 2412 native code library failed to load:" + e);
        }
        logger.info("OpenCVLoader finished loading 2412.");
    }
}
