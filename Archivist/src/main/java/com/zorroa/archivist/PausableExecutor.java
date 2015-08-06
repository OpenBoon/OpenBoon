package com.zorroa.archivist;

/**
 * Created by chambers on 7/31/15.
 */

import com.zorroa.archivist.domain.IngestProcessorFactory;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PausableExecutor extends ThreadPoolExecutor {
    private final Continue cont = new Continue();

    private List<IngestProcessorFactory> processors;

    public PausableExecutor(int threads) {
        super(threads, threads, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public void pause() {
        cont.pause();
    }

    public void resume() {
        cont.resume();
    }

    public List<IngestProcessorFactory> getProcessors() {
        return processors;
    }

    public void setProcessors(List<IngestProcessorFactory> processors) {
        this.processors = processors;
    }

    protected void beforeExecute(Thread t, Runnable r) {
        try {
            cont.checkIn();
        } catch (InterruptedException e) {
            return;
        }
        super.beforeExecute(t, r);
    }

    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t != null) {
            // Teardown processors if we have an exception during execution
            for (IngestProcessorFactory processorFactory : processors) {
                if (processorFactory.getProcessor() != null) {
                    processorFactory.getProcessor().teardown();
                }
            }
        }
    }
}