package com.zorroa.archivist.sdk.domain;

import java.util.Map;

/**
 * An ExportProcessor is for defining a self contained piece of business logic
 * for use within a Export Pipeline.
 */
public abstract class ImageAssetExportOptions {

    /**
     * Strip off all existing image attributes.
     */
    private boolean stripMetdata = false;

    /**
     * The text for the copy right field, if any.
     */
    private String copyrightText;

    /**
     * A map of new arbitrary attributes to set.
     */
    private Map<String, String> attrs;

    /**
     * Image scale.  100 or <=0 is to leave as is.
     */
    private double scale = 100.0;

    /**
     * The quality of the image, if supported. 100 or <= will leave as is.
     */
    private double quality = 100.0;

    /**
     * The format of the images, null to keep existing format.
     */
    private String format;

    public ImageAssetExportOptions() { }
    
}
