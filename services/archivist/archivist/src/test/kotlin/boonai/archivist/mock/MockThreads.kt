package boonai.archivist.mock

import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.util.concurrent.FailureCallback
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.util.concurrent.ListenableFutureCallback
import org.springframework.util.concurrent.SuccessCallback
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Mock unittest implementations for AsyncListenableTaskExecutors.  The reason these exist
 * is because SimpleAsyncListenableTaskExecutor doesn't give us inline execution for
 * unittests which makes things very hard to test.
 */

class MockListenableFuture<T> constructor(private val value: T) : ListenableFuture <T> {

    override fun get(): T = value

    //
    // None of these are used, currently.
    //
    override fun addCallback(p0: ListenableFutureCallback<in T>) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun addCallback(p0: SuccessCallback<in T>, p1: FailureCallback) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun isDone(): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun get(timeout: Long, unit: TimeUnit?): T {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun isCancelled(): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}

class MockAsyncThreadExecutor : SyncTaskExecutor(), AsyncListenableTaskExecutor {

    override fun <T : Any?> submitListenable(p0: Callable<T>): ListenableFuture<T> {
        val result = p0.call()
        return MockListenableFuture(result)
    }

    //
    // None of these are used, currently.
    //
    override fun execute(p0: Runnable, p1: Long) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun submit(p0: Runnable): Future<*> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> submit(p0: Callable<T>): Future<T> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun submitListenable(p0: Runnable): ListenableFuture<*> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
