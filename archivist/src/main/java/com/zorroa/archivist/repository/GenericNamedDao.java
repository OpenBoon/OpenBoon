package com.zorroa.archivist.repository;

/**
 * Created by chambers on 7/9/16.
 */
public interface GenericNamedDao<T, S> extends GenericDao<T, S> {

    T get(String name);

    boolean exists(String name);
}
