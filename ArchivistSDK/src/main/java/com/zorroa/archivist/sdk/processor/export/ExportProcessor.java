package com.zorroa.archivist.sdk.processor.export;

import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportOutput;
import com.zorroa.archivist.sdk.domain.ExportedAsset;
import com.zorroa.archivist.sdk.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chambers on 11/13/15.
 */
public abstract class ExportProcessor extends Processor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public abstract void init(Export export, ExportOutput output) throws Exception;

    public abstract void process(ExportedAsset asset) throws Exception;

    public abstract String getMimeType();

    public abstract String getFileExtension();
}
