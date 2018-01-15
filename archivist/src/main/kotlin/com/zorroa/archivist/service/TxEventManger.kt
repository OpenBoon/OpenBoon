package com.zorroa.archivist.service

import com.google.common.base.Preconditions
import com.zorroa.archivist.service.TransactionEventManager.Companion.executor
import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl.ThreadStateMap.Byte0.runnable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TransactionEventManager {

    /**
     * Immediate mode ensures synchronizations are executed immediately upon
     * registration.
     */
    var isImmediateMode = false

    /**
     * Queue up and AfterCommit runnable.
     * @param body
     */
    fun afterCommit(sync: Boolean=true, body: () -> Unit) {
        register(AfterCommit(sync, body))
    }

    fun register(txs: BaseTransactionSynchronization) {
        Preconditions.checkNotNull(runnable, "The AsyncTransactionSynchronization cannot be null")
        if (isImmediateMode) {
            try {
                txs.body()
            } catch (e: Exception) {
                logger.warn("Failed to execute TransactionSynchronization in immediate mode, " + e.message, e)
            }

        } else {
            TransactionSynchronizationManager.registerSynchronization(txs)
        }
    }
    companion object {
        private val logger = LoggerFactory.getLogger(TransactionEventManager::class.java)

        val executor :ExecutorService = Executors.newFixedThreadPool(4)
    }
}

open class BaseTransactionSynchronization(val body: () -> Unit) : TransactionSynchronization {
    override fun suspend() { }
    override fun beforeCompletion() { }
    override fun afterCommit() { }
    override fun resume() { }
    override fun flush() { }
    override fun beforeCommit(p0: Boolean) {}
    override fun afterCompletion(p0: Int) { }
}

class AfterCommit(private val sync: Boolean, body: () -> Unit) : BaseTransactionSynchronization(body) {

    override fun afterCommit() = if (sync) {
        body()
    }
    else {
        executor.execute({ body() })
    }
}
