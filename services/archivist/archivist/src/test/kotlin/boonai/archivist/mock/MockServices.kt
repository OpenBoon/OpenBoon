package boonai.archivist.mock

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.core.task.AsyncListenableTaskExecutor

/**
 * Replacement mockito.any method that handles methods that
 * don't accept null.  Casts an Any object to the given type.
 */
fun <T> zany(clazz: Class<T>): T {
    val any: Any? = Mockito.any()
    return clazz.cast(any)
}

/**
 * Replacement mockito.any method that handles methods that
 * don't accept null.  Casts an Any object to an inferred type.
 */
fun <T> zany(): T {
    val any: Any? = Mockito.any()
    return any as T
}

@Configuration
@Order(-1)
class MockServiceConfiguration {

    /**
     * Return a mock work queue which runs all tasks in the current thread.
     */
    @Bean
    @Primary
    fun mockWorkQueue(): AsyncListenableTaskExecutor {
        return MockAsyncThreadExecutor()
    }
}
