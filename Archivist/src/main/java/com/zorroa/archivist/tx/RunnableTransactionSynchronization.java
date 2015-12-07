package com.zorroa.archivist.tx;

/**
 * Created by chambers on 12/7/15.
 */
public interface RunnableTransactionSynchronization extends Runnable  {

    default boolean isAsync() {
        return true;
    }
}
