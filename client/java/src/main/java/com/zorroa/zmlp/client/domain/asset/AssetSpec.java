package com.zorroa.zmlp.client.domain.asset;

import com.zorroa.zmlp.client.domain.Clip;

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

    public AssetSpec setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public Map getAttrs() {
        return attrs;
    }

    public AssetSpec setAttrs(Map attrs) {
        this.attrs = attrs;
        return this;
    }

    public Clip getClip() {
        return clip;
    }

    public AssetSpec setClip(Clip clip) {
        this.clip = clip;
        return this;
    }
}
