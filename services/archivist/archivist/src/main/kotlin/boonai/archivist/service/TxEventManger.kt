package boonai.archivist.service

import com.google.common.base.Preconditions
import boonai.archivist.config.ArchivistConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

class TransactionEventManager {

    @Autowired
    lateinit var workQueue: AsyncListenableTaskExecutor

    /**
     * Immediate mode ensures synchronizations are executed immediately upon
     * registration.
     */
    var isImmediateMode = ArchivistConfiguration.unittest

    /**
     * Queue up and AfterCommit runnable.
     * @param body
     */
    fun afterCommit(sync: Boolean = true, body: () -> Unit) {
        register(AfterCommit(sync, workQueue, body))
    }

    fun register(txs: BaseTransactionSynchronization) {
        Preconditions.checkNotNull(txs, "The BaseTransactionSynchronization cannot be null")
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

class AfterCommit(private val sync: Boolean, val workQueue: AsyncListenableTaskExecutor, body: () -> Unit) : BaseTransactionSynchronization(body) {

    val ctx = SecurityContextHolder.getContext()

    override fun afterCommit() {
        if (sync) {
            body()
        } else {
            workQueue.execute(
                DelegatingSecurityContextRunnable(
                    Runnable {
                        body()
                    },
                    ctx
                )
            )
        }
    }
}
