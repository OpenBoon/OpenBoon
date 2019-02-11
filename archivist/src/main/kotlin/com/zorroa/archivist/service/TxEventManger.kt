package com.zorroa.archivist.service

import com.google.common.base.Preconditions
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.security.getAuthentication
import com.zorroa.archivist.security.withAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

class TransactionEventManager {

    /**
     * Immediate mode ensures synchronizations are executed immediately upon
     * registration.
     */
    var isImmediateMode = ArchivistConfiguration.unittest

    /**
     * Queue up and AfterCommit runnable.
     * @param body
     */
    fun afterCommit(sync: Boolean=true, body: () -> Unit) {
        register(AfterCommit(sync, body))
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

class AfterCommit(private val sync: Boolean, body: () -> Unit) : BaseTransactionSynchronization(body) {

    val auth: Authentication? = getAuthentication()

    override fun afterCommit() {
        if (sync) {
            body()
        } else {
            GlobalScope.launch(Dispatchers.Default) {
                withAuth(auth) {
                    body()
                }
            }
        }
    }
}
