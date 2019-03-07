package com.zorroa.archivist.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.security.core.Authentication
import java.util.concurrent.TimeUnit

enum class LockStatus {
    Locked,
    Combined,
    Wait
}

/**
 * ClusterLockSpec defines all possible properties for creating a cluster lock.
 *
 * @property name The name of the lock.
 * @property maxTries The maximum number of times to attempt to lock the name.  -1 for infinite.
 * @property combineMultiple Combine this lock with an active combine lock of the same name
 * @property timeout The overall timeout for the lock.
 * @property timeoutUnits The unit of time for the timeout value.
 * @property holdTillTimeout Hold the lock until it times out, even if the work is done.
 * @property dispatcher The Coroutine dispatcher to use.
 *
 */
class ClusterLockSpec(
        var name : String,
        var maxTries: Int = 0,
        var combineMultiple: Boolean = false,
        var timeout : Long = 1,
        var timeoutUnits : TimeUnit = TimeUnit.MINUTES,
        var holdTillTimeout: Boolean = false,
        var dispatcher: CoroutineDispatcher = Dispatchers.Default,
        var authentication: Authentication? = null) {

    companion object {

        /**
         * A CombineLock will be merged into the a single lock. When a combine lock is
         * taken, if a lock already exists, the existing thread will simply execute the same code
         * again.
         *
         * When using a combine lock, you must ensure that the logic being executed is the same
         * in all cases. Otherwise, the original lock would execute the wrong logic. You can
         * have both combine locks and non-combine locks with the same name.
         *
         * @param name The name of the lock
         * @return A ClusterLockSpec
         */
        fun combineLock(name: String) : ClusterLockSpec =
                ClusterLockSpec(name, combineMultiple=true, maxTries=-1)

        /**
         * A hard lock is a guarantee that at some point the code will be executed, barring
         * a server crash or something of that nature.
         *
         * @param name The name of the lock
         * @return A ClusterLockSpec
         */
        fun hardLock(name: String) : ClusterLockSpec =
                ClusterLockSpec(name, maxTries=-1)

        /**
         * A soft lock will attempt to take a lock, but stop doing so upon the first failure.
         */
        fun softLock(name: String, maxTries: Int=0) : ClusterLockSpec =
                ClusterLockSpec(name, maxTries)
    }
}


