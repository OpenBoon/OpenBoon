package com.zorroa.cloudproxy.domain;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 4/19/17.
 */
public class AssetExecutor  extends ThreadPoolExecutor {

    public AssetExecutor(int threads) {
        super(threads, threads, 60, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
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
