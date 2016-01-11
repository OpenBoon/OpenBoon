package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.*;

import java.util.List;

/**
 * Created by chambers on 11/1/15.
 */
public interface ExportService {

    Export create(ExportBuilder builder);

    Export get(int id);

    List<ExportOutput> getAllOutputs(Export export);

    ExportOutput getOutput(int id);

    List<Export> getAll(ExportFilter filter);

    /**
     * Restart a given export.  The export will be set back into into the queued
     * state and will export the exact same files as the original run.
     *
     * @param export
     */
    void restart(Export export);

    /**
     * Duplicates a given export.  The new export will be in the queued state
     * and execute the exact same search as the original export. The result
     * of the new export may be different from the original.
     *
     * @param export
     */
    Export duplicate(Export export);

    /**
     * Cancel the export.  This will set the export state to Cancelled.  A Running
     * export will be stopped immediately, however all the export processor tear
     * down functions will run.
     *
     * @param export
     * @return
     */
    boolean cancel(Export export);

    /**
     * Offline a given export output.
     *
     * @param output
     * @return
     */
    boolean offline(ExportOutput output);

    /**
     * Offline and entire export.  Returns the number of exports offlined.
     *
     * @param export
     * @return
     */
    public int offline(Export export);

    /**
     * Get or create a folder for this export.
     *
     * @param export
     * @return
     */
    public Folder folder(Export export);
}
