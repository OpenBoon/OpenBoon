package com.zorroa.archivist.tx;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Provides ability to execute callbacks at particular transaction states.  Most notably,
 * the TransactionEventManager allows for the definition of callbacks from within a transactional
 * context, however the code may run outside of the transaction.
 *
 */
public class TransactionEventManager {

    private static final Logger logger = LoggerFactory.getLogger(TransactionEventManager.class);

    private final Executor executor = Executors.newFixedThreadPool(4);

    /**
     * Immediate mode ensures synchronizations are executed immediately upon
     * registration.
     */
    private boolean immediateMode = false;

    /**
     * Queue up and AfterCommitSync which executes inline with original request.
     */
    public void afterCommitSync(AfterCommit runnable) {
        register(runnable, false);
    }

    /**
     * Queue up and AfterCommit runnable and override the isAsync() method with the given boolean.
     * @param runnable
     * @param async
     */
    public void afterCommit(AfterCommit runnable, boolean async) {
        register(runnable, async);
    }

    /**
     * Queue up and AfterCommit runnable.
     * @param runnable
     */
    public void afterCommit(AfterCommit runnable) {
        register(runnable, runnable.isAsync());
    }

    public void register(RunnableTransactionSynchronization runnable, boolean async) {
        Preconditions.checkNotNull(runnable, "The AsyncTransactionSynchronization cannot be null");
        if (isImmediateMode()) {
            try {
//                logger.info("Executing TXS in immedate  mode: '{}'", runnable.getClass().getCanonicalName());
                runnable.run();
            }
            catch (Exception e) {
                logger.warn("Failed to execute TransactionSynchronization in immediate mode, " + e.getMessage(), e);
            }
        }
        else {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationWrapper(runnable, async));
        }
    }

    private class TransactionSynchronizationWrapper implements TransactionSynchronization {

        private final RunnableTransactionSynchronization runnable;
        private final boolean async;

        public TransactionSynchronizationWrapper(RunnableTransactionSynchronization runnable, boolean async) {
            this.runnable = runnable;
            this.async = async;
        }

        @Override
        public void suspend() {

        }

        @Override
        public void resume() {

        }

        @Override
        public void flush() {

        }

        @Override
        public void beforeCommit(boolean b) {

        }

        @Override
        public void beforeCompletion() {

        }

        @Override
        public void afterCommit() {
            if (runnable instanceof AfterCommit) {
                if (runnable.isAsync()) {
                    runnable.run();
                }
                else {
                    executor.execute(runnable);
                }
            }
        }

        @Override
        public void afterCompletion(int i) {

        }
    }

    public boolean isImmediateMode() {
        return immediateMode;
    }

    public void setImmediateMode(boolean immediateMode) {
        this.immediateMode = immediateMode;
    }
}
