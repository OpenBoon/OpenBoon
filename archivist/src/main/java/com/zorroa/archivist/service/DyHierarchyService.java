package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.DyHierarchy;
import com.zorroa.archivist.domain.DyHierarchySpec;

import java.util.concurrent.Future;

/**
 * Created by chambers on 7/14/16.
 */
public interface DyHierarchyService {

    boolean update(DyHierarchy dyhi, DyHierarchySpec spec);

    /**
     * Create a dynamic hierarchy generator.
     *
     * @param spec
     */
    DyHierarchy create(DyHierarchySpec spec);

    /**
     * Generate a dynamic hierarchy.
     *
     * @param agg
     */
    int generate(DyHierarchy agg);

    /**
     * Generate all hierarchies.
     */
    void generateAll();

    /**
     * Submit command to generate folders on all hierarchies.  This command
     * is throttled to a reasonable rate.  To bypass the throttle, set the
     * 'force' argument to true.
     *
     * @param force
     */
    void submitGenerateAll(boolean force);

    /**
     * Generate a dynamic hierarchy.
     *
     * @param agg
     */
    Future<Integer> submitGenerate(DyHierarchy agg);
}
