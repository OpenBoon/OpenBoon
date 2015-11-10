package com.zorroa.archivist.sdk.processor.export;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.DuplicateElementException;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.processor.Processor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An ExportProcessor is for defining a self contained piece of business logic
 * for use within a Export Pipeline.
 */
public abstract class ExportProcessor extends Processor {

    public ExportProcessor() { }


    protected abstract void process(Asset asset, Export export, String workingDirectory) throws Exception;

}
