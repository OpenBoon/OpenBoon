package com.zorroa.archivist.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UniqueTaskExecutor  {

    private static final Logger logger = LoggerFactory.getLogger(UniqueTaskExecutor.class);

    private final SubExecutor subExecutor;

    public UniqueTaskExecutor() {
        subExecutor = new SubExecutor(false);
    }

    public UniqueTaskExecutor(boolean sync) {
        subExecutor = new SubExecutor(sync);
    }

    public boolean execute(final UniqueRunnable task) {
        return subExecutor.execute(task);
    }

    public void execute(final Runnable task) {
        subExecutor.execute(task);
    }

    private class SubExecutor extends ThreadPoolExecutor {
        private ConcurrentHashMap<Runnable, Integer> map = new ConcurrentHashMap<>();

        private boolean sync = false;

        public SubExecutor() {
            super(1, 1, 365, TimeUnit.DAYS, new LinkedBlockingQueue(),
                    new ThreadPoolExecutor.DiscardPolicy());
        }

        public SubExecutor(boolean sync) {
            this();
            this.sync = sync;
        }
        /**
         * Remove the runnable right before executing.
         *
         * @param t
         * @param r
         */
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            map.remove(r);
        }

        public boolean execute(final UniqueRunnable task) {
            if (sync) {
                logger.info("Running task inline");
                task.run();
                return true;
            }
            else {
                if (map.putIfAbsent(task, 1) != null) {
                    return false;
                }
                super.execute(task);
                return true;
            }
        }

        @Override
        public void execute(final Runnable task) {
            if (sync) {
                logger.info("Running task inline");
                task.run();
            }
            else {
                super.execute(task);
            }
        }
    }
}
