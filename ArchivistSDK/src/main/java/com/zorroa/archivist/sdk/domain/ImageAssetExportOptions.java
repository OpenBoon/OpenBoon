package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.DuplicateElementException;
import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportData;
import com.zorroa.archivist.sdk.processor.Processor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An ExportProcessor is for defining a self contained piece of business logic
 * for use within a Export Pipeline.
 */
public abstract class ImageAssetExportOptions {

    private boolean stripMetdata;

    /**
     * The text for the copy right field, if any.
     */
    private String copyrightText;

    /**
     * A map of new arbitrary attributes to set.
     */
    private Map<String, String> attrs;

    /**
     * Image scale.
     */
    private double scale = 100.0;


    private double quality = 100.0;


    public ImageAssetExportOptions() { }




}
