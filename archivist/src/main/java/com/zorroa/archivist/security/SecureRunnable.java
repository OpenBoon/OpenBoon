package com.zorroa.archivist.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Created by chambers on 4/27/17.
 */
public class SecureRunnable implements Runnable {

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
