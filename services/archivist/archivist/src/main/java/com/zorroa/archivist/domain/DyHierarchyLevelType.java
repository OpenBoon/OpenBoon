package com.zorroa.archivist.domain;

/**
 * Created by chambers on 7/14/16.
 */
public enum DyHierarchyLevelType {
    /**
     * A simple attribute.
     */
    Attr,

    /**
     * The year, formatted as 'yyyy'
     */
    Year,

    /**
     * The month, formatted as 'M'
     */
    Month,

    /**
     * The day, formatted as 'd'
     */
    Day,

    /**
     * A Path of some type with a delimiter.
     */
    Path
}
