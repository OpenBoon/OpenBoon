package com.zorroa.archivist;

import com.zorroa.archivist.domain.UniqueRunnable;
import com.zorroa.archivist.domain.UniqueTaskExecutor;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UniqueTaskExecutorTests {

    @Test
    public void testExecute() throws InterruptedException {
        UniqueTaskExecutor t = new UniqueTaskExecutor();
        boolean result = t.execute(new UniqueRunnable("butt", () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
        assertTrue(result);
        Thread.sleep(100);

        result = t.execute(new UniqueRunnable("butt", () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
        assertTrue(result);

        result = t.execute(new UniqueRunnable("butt", () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
        assertFalse(result);
    }


}
