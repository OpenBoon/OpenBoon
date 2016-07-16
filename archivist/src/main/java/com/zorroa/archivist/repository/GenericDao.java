package com.zorroa.archivist.repository;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;

import java.util.List;

/**
 * Created by chambers on 7/15/16.
 */
public interface GenericDao<T, S> {

    T create(S spec);

    T get(int id);

    T refresh(T object);

    List<T> getAll();

    PagedList<T> getAll(Paging paging);

    boolean update(int id, S spec);

    boolean delete(int id);

    long count();
}
