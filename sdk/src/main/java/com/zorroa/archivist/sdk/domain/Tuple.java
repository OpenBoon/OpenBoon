package com.zorroa.archivist.sdk.domain;

import java.util.Objects;

/**
 * Created by chambers on 4/11/16.
 */
public class Tuple<LEFT, RIGHT> {

    private final LEFT left;
    private final RIGHT right;

    public Tuple(LEFT left, RIGHT right) {
        this.left = left;
        this.right = right;
    }

    public RIGHT getRight() {
        return right;
    }

    public LEFT getLeft() {
        return left;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple<?, ?> tuple = (Tuple<?, ?>) o;
        return Objects.equals(getLeft(), tuple.getLeft()) &&
                Objects.equals(getRight(), tuple.getRight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLeft(), getRight());
    }
}
