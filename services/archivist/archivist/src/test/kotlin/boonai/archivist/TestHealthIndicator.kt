package boonai.archivist

import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.stereotype.Component

/**
 * An externally mutable AbstractHealthIndicator which lets us change
 * the health of the server for unit tests.
 */
@Component
class TestHealthIndicator : AbstractHealthIndicator() {

    private var healthy = true

    @Throws(Exception::class)
    override fun doHealthCheck(builder: Health.Builder) {
        if (healthy) {
            builder.up()
        } else {
            builder.down()
        }
    }

    fun isHealthy(): Boolean {
        return healthy
    }

    fun setHealthy(healthy: Boolean): TestHealthIndicator {
        this.healthy = healthy
        return this
    }
}
