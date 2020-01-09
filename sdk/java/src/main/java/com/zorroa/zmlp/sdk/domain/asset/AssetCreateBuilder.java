package com.zorroa.zmlp.sdk.domain.asset;

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

    /**
     *
     * Set Analyze Attribute and returns the same instance.
     *
     * @param analyze
     * @return
     */
    public AssetCreateBuilder withAnalyze(Boolean analyze){
        this.analyze = analyze;
        return this;
    }

    /**
     * Set Assets attribute and returns the same instance.
     *
     * @param assetsSpecList
     * @return
     */
    public AssetCreateBuilder withAssets(List<AssetSpec> assetsSpecList){
        this.assets = assetsSpecList;
        return this;
    }

    public List<AssetSpec> getAssets() {
        return assets;
    }

    public void setAssets(List<AssetSpec> assets) {
        this.assets = assets;
    }

    public Boolean getAnalyze() {
        return analyze;
    }

    public void setAnalyze(Boolean analyze) {
        this.analyze = analyze;
    }


}
