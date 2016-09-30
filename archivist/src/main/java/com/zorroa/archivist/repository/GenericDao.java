package com.zorroa.archivist.repository;

import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.util.List;

/**
 * Created by chambers on 7/15/16.
 */
public interface GenericDao<T, S> {

    T create(S spec);

    T get(int id);

    T refresh(T object);

    List<T> getAll();

    PagedList<T> getAll(Pager paging);

    boolean update(int id, T spec);

    boolean delete(int id);

    long count();
}
