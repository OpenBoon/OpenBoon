package com.zorroa.archivist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 12/15/16.
 */
public class SecureSingleThreadExecutor extends ThreadPoolExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SecureSingleThreadExecutor.class);

    public static final SecureSingleThreadExecutor singleThreadExecutor() {
        return new SecureSingleThreadExecutor(1, 1,
                        0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>());
    }

    public SecureSingleThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    public void execute(final Runnable r) {
        super.execute(new SecureRunnable(r, SecurityContextHolder.getContext()));
    }

    private class SecureRunnable implements Runnable {

        private final Runnable delegate;
        private final SecurityContext context;

        public SecureRunnable(Runnable runnable, SecurityContext ctx) {
            this.delegate = runnable;
            this.context = ctx;
        }

        @Override
        public void run() {
            try {
                SecurityContextHolder.setContext(context);
                delegate.run();
            }
            finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

}
