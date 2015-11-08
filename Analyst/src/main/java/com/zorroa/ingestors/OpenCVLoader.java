/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.ingestors;

/**
 * Statically loads the shared OpenCV java classes to avoid having
 * link collisions when separate plugins load the same dylib.
 * Requires a shared ProcessorFactory<> classLoader to avoid
 * re-loading the static OpenCVLoader libraries.
 */
public class OpenCVLoader {
    static {
        try {
            System.loadLibrary("opencv_java2411");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("OpenCV 2411 native code library failed to load.\n" + e);
        }
    }
}
