package com.zorroa.archivist.domain;

import com.zorroa.sdk.processor.ProcessorRef;

import java.util.Map;

/**
 * Utilized when defining Processor references packaged either with the SDK.
 */
public class SdkProcessorRef extends ProcessorRef {

    public SdkProcessorRef() {
        this.setLanguage("java");
    }

    public SdkProcessorRef(String name) {
        this.setClassName(name);
        this.setLanguage("java");
    }

    public SdkProcessorRef(String name, Map<String, Object> args) {
        this.setClassName(name);
        this.setArgs(args);
        this.setLanguage("java");
    }
}
