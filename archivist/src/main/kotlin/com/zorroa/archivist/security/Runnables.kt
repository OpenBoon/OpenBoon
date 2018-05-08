package com.zorroa.archivist.security

import com.zorroa.archivist.config.ArchivistConfiguration
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class InternalRunnable(private val auth: Authentication, private val body: () -> Unit) : Runnable {

    override fun run() {
        if (ArchivistConfiguration.unittest) {
            body()
        } else {
            try {
                SecurityContextHolder.getContext().authentication = auth
                body()
            } finally {
                /**
                 * Don't clear this on unit tests.
                 */
                if ("main" == Thread.currentThread().name) {
                    logger.info("Main thread, not clearing security context")
                } else {
                    SecurityContextHolder.clearContext()
                }
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(InternalRunnable::class.java)
    }
}

class SecureRunnable(private val context: SecurityContext, private val body: () -> Unit) : Runnable {

    override fun run() {
        try {
            SecurityContextHolder.setContext(context)
            body()
        } finally {
            /**
             * Don't clear this on unit tests.
             */
            if ("main" == Thread.currentThread().name) {
                logger.info("Main thread, not clearing security context")
            } else {
                SecurityContextHolder.clearContext()
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SecureRunnable::class.java)
    }
}

class SecureSingleThreadExecutor(corePoolSize: Int, maximumPoolSize: Int, keepAliveTime: Long, unit: TimeUnit, workQueue: BlockingQueue<Runnable>) : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue) {

    override fun execute(r: Runnable) {
        super.execute(SecureRunnable(r, SecurityContextHolder.getContext()))
    }

    private inner class SecureRunnable(private val delegate: Runnable, private val context: SecurityContext) : Runnable {

        override fun run() {
            try {
                SecurityContextHolder.setContext(context)
                delegate.run()
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SecureSingleThreadExecutor::class.java)

        fun singleThreadExecutor(): SecureSingleThreadExecutor {
            return SecureSingleThreadExecutor(1, 1,
                    0L, TimeUnit.MILLISECONDS,
                    LinkedBlockingQueue())
        }
    }

}
