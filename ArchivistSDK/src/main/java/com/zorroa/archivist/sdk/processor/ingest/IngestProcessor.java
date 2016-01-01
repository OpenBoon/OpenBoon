package com.zorroa.archivist.sdk.processor.ingest;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.processor.Processor;

public abstract class IngestProcessor extends Processor {

    public IngestProcessor() { };

    public abstract void process(AssetBuilder asset);

    public void init(Ingest ingest) { };
}
