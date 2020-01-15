package com.zorroa.zmlp.client.domain.asset;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the properties necessary to provision a batch of assets.
 */
public class AssetCreateBuilder {

    /**
     * The list of assets to be created
     */
    private List<AssetSpec> assets;

    /**
     * Set to true if the assets should undergo further analysis, or false to stay in the provisioned state.
     */
    private Boolean analyze;

    public AssetCreateBuilder() {
        assets = new ArrayList();
    }

    /**
     *
     * @param assetSpec
     * @return
     */
    public AssetCreateBuilder addAsset(AssetSpec assetSpec){
        this.assets.add(assetSpec);
        return this;
    }

    /**
     * Add an AssetSpec to Assets List
     *
     * @param assetSpecUrl
     * @return
     */
    public AssetCreateBuilder addAsset(String assetSpecUrl) {
        this.assets.add(new AssetSpec(assetSpecUrl));
        return this;
    }

    public List<AssetSpec> getAssets() {
        return assets;
    }

    public AssetCreateBuilder setAssets(List<AssetSpec> assets) {
        this.assets = assets;
        return this;
    }

    public Boolean getAnalyze() {
        return analyze;
    }

    public AssetCreateBuilder setAnalyze(Boolean analyze) {
        this.analyze = analyze;
        return this;
    }
}
