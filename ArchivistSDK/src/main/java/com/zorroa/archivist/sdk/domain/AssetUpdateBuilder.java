package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Lists;

import java.util.List;

/**
 *
 */
public class AssetUpdateBuilder {

    /**
     * Tweaks are changes to ingested data that we want to preserve
     * even if the data gets re-ingested.
     */
    private List<Tweak> tweaks = Lists.newArrayList();


    /**
     * Permissions
     */

    /**
     *
     */

    /**
     * Remove a keyword
     */

    public enum TweakOp {
        RemoveField,
        RemoveValue,
        RemoveAsset,
        MergeAsset,
        SetValue,
    }

    private static class Tweak {
        private TweakOp op;
        private String field;
        private Object value;
    }
}
