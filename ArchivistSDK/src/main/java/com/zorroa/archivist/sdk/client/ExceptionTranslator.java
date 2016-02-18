package com.zorroa.archivist.sdk.client;

/**
 * Created by chambers on 2/18/16.
 */
public class ExceptionTranslator {

    public static void translate(Throwable e) {
        throw new RuntimeException(e);
    }
}
