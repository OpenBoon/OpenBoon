package com.zorroa.archivist.security;

import com.zorroa.archivist.config.ArchivistConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Created by chambers on 4/27/17.
 */
public class InternalRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(InternalRunnable.class);

    private final Runnable delegate;
    private final Authentication auth;

    public InternalRunnable(Authentication auth, Runnable runnable) {
        this.delegate = runnable;
        this.auth = auth;
    }

    @Override
    public void run() {
        if (ArchivistConfiguration.unittest) {
            delegate.run();
        }
        else {
            try {
                SecurityContextHolder.getContext().setAuthentication(auth);
                delegate.run();
            } finally {
                /**
                 * Don't clear this on unit tests.
                 */
                if ("main".equals(Thread.currentThread().getName())) {
                    logger.info("Main thread, not clearing security context");
                } else {
                    SecurityContextHolder.clearContext();
                }
            }
        }
    }
}
