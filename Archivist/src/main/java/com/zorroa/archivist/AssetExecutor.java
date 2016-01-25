package com.zorroa.archivist;

/**
 * Created by chambers on 7/31/15.
 */

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AssetExecutor extends ThreadPoolExecutor {

    public AssetExecutor(int threads) {
        super(threads, threads, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        this.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("ASSET WORKER-%d").build());
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
