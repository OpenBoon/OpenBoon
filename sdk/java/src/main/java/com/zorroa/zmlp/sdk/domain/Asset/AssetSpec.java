package com.zorroa.zmlp.sdk.domain.Asset;

import com.zorroa.zmlp.sdk.domain.Clip;

import java.util.Map;

/**
 * Defines all the properties required to create an Asset.
 */
public class AssetSpec {

    /**
     * a URI locator to the file asset.
     */
    private String uri;

    /**
     * A shallow key/value pair dictionary of starting point attributes to set on the asset.
     */
    private Map attrs;

    /**
     * Defines a subset of the asset to be processed, for example a page of a PDF or time code from a video.
     */
    private Clip clip;

    public AssetSpec() {
    }

    public AssetSpec(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Map getAttrs() {
        return attrs;
    }

    public void setAttrs(Map attrs) {
        this.attrs = attrs;
    }

    public Clip getClip() {
        return clip;
    }

    public void setClip(Clip clip) {
        this.clip = clip;
    }
}
