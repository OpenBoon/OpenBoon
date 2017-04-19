package com.zorroa.cloudproxy.domain;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 4/19/17.
 */
public class ImportExecutor extends ThreadPoolExecutor {

    public ImportExecutor(int threads) {
        super(threads, threads, 60, TimeUnit.MINUTES, new LinkedBlockingQueue<>(100));
        this.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("ASSET_WORKER-%d").build());
        this.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    }

    public void waitForCompletion() {
        shutdown();
        try {
            awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ignore) {
            // most likely means the server is being shutdown, can ignore
        }
    }
}
