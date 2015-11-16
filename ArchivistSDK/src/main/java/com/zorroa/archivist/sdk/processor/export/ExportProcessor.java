package com.zorroa.archivist.sdk.processor.export;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chambers on 11/13/15.
 */
public abstract class ExportProcessor extends Processor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public void init(Export export, ExportOutput output, String outputDir) throws Exception { }

    public abstract void process(Asset asset) throws Exception;
}
