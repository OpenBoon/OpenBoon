package com.zorroa.archivist.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Created by chambers on 4/27/17.
 */
public class SecureRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SecureRunnable.class);

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
            /**
             * Don't clear this on unit tests.
             */
            if ("main".equals(Thread.currentThread().getName())) {
                logger.info("Main thread, not clearing security context");
            }
            else {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
