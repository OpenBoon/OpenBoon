package com.zorroa.archivist;

/**
 * Created by chambers on 7/31/15.
 */
import java.util.concurrent.*;

public class PausableExecutor extends ThreadPoolExecutor {
    private final Continue cont = new Continue();

    public PausableExecutor(int threads) {
        super(threads, threads, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public void pause() {
        cont.pause();
    }

    public void resume() {
        cont.resume();
    }

    protected void beforeExecute(Thread t, Runnable r) {
        try {
            cont.checkIn();
        } catch (InterruptedException e) {
            return;
        }
        super.beforeExecute(t, r);
    }
}