package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.*;

import java.util.List;

/**
 * Created by chambers on 11/12/15.
 */
public interface ExportDao {

    Export get(int id);

    List<Export> getAll(ExportFilter filter);

    Export create(ExportBuilder builder, long totalFileSize, long assetCount);

    List<Export> getAll(ExportState state, int limit);

    boolean setQueued(Export export);

    /**
     * Set the given export state to ExportState.Running.  This also updates the started time
     * to the current timestamp, resets the stop time to -1, and increments the execution counter.
     *
     * @param export
     * @return
     */
    boolean setRunning(Export export);

    /**
     * Set the given export state to ExportState.Finished.  This also updates the started time
     * to the current timestamp, resets the stop time to -1, and increments the execution counter.
     *
     * @param export
     * @return
     */
    boolean setFinished(Export export);

    boolean setCancelled(Export export);

    boolean setState(Export export, ExportState newState, ExportState oldState);

    /**
     * Reset the search an export is going to do.  This can only be done on queued exports.
     *
     * @param export
     * @param search
     * @return
     */
    boolean setSearch(Export export, AssetSearch search);

    boolean isInState(Export export, ExportState state);
}
