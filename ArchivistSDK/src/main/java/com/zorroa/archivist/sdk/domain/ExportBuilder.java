package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Created by chambers on 11/1/15.
 */
public class ExportBuilder {

    /**
     * The export pipeline the assets should be run through.
     */
    private int exportPipelineId;

    /**
     * The search used to produce a list of exported assets.
     */
    private AssetSearchBuilder search;

}
