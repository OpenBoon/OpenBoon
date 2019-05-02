package com.zorroa.archivist.domain;

import java.util.Objects;

public class UniqueRunnable implements Runnable {

    private final String key;
    private final Runnable runnable;

    public UniqueRunnable(String key, Runnable runnable) {
        this.key = key;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        runnable.run();
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UniqueRunnable that = (UniqueRunnable) o;
        return Objects.equals(getKey(), that.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey());
    }
}
