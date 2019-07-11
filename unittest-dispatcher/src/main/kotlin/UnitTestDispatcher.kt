package com.zorroa.unittest

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.internal.MainDispatcherFactory
import kotlin.coroutines.CoroutineContext

/**
 * Returns a dispatcher that only supports immediate exceution in the current context.
 * Used for unittest to ensure that Spring @Transactional db semantics are supported.
 * Use in cases where concurrency is required will fail, possibly in weird ways.
 */
@InternalCoroutinesApi
internal class UnitTestDispatcherFactory : MainDispatcherFactory {
    override fun createDispatcher(): MainCoroutineDispatcher = UnitTestMainDispatcher
    override val loadPriority: Int
        get() = Int.MAX_VALUE
}

private object UnitTestMainDispatcher : MainCoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        throw UnsupportedOperationException("Unittest Main dispatcher not supported, please use Dispatchers.Main.immediate")
    }

    override val immediate: MainCoroutineDispatcher
        get() = UnitTestMainImmediateDispatcher

    override fun toString() = "UnitTest Main"
}

private object UnitTestMainImmediateDispatcher : MainCoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        // This should never happen in practice since isDispatchNeeded returns false.
        // See [isDispatchNeeded] documentation
        throw UnsupportedOperationException("Unittest dispatcher only supports executing in the same thread.")
    }

    override val immediate: MainCoroutineDispatcher
        get() = this

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = false

    override fun toString() = "UnitTest Main [immediate]"
}
