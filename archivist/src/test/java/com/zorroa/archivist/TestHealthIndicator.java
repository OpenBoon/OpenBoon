package com.zorroa.archivist;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * An externally mutable AbstractHealthIndicator which lets us change
 * the health of the server for unit tests.
 */
@Component
public class TestHealthIndicator extends AbstractHealthIndicator {

    private boolean healthy = true;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        if (healthy) {
            builder.up();
        }
        else {
            builder.down();
        }
    }

    public boolean isHealthy() {
        return healthy;
    }

    public TestHealthIndicator setHealthy(boolean healthy) {
        this.healthy = healthy;
        return this;
    }
}
