package com.zorroa.archivist;

/**
 * Created by chambers on 7/31/15.
 */

import com.zorroa.archivist.domain.IngestProcessorFactory;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AssetExecutor extends ThreadPoolExecutor {
    private List<IngestProcessorFactory> processors;

    public AssetExecutor(int threads) {
        super(threads, threads, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public void setProcessors(List<IngestProcessorFactory> processors) {
        this.processors = processors;
    }

    public void teardownProcessors() {
        for (IngestProcessorFactory processorFactory : processors) {
            if (processorFactory.getProcessor() != null) {
                processorFactory.getProcessor().teardown();
            }
        }
    }

    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t != null) {
            // Teardown processors if we have an exception during execution
            teardownProcessors();
        }
    }
}