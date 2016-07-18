package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.DyHierarchy;
import com.zorroa.archivist.domain.DyHierarchySpec;

/**
 * Created by chambers on 7/14/16.
 */
public interface DyHierarchyDao extends GenericDao<DyHierarchy, DyHierarchySpec> {


    boolean isWorking(DyHierarchy d);

    boolean setWorking(DyHierarchy d, boolean value);
}
