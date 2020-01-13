package com.zorroa.zmlp.client.domain.asset;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the properties required to batch upload a list of assets.
 */
public class AssetUploadBuilder {

    /**
     * A list of AssetSpec objects which define the Assets starting metadata.
     */
    private List<AssetSpec> assets;

    /**
     * Set to true if the assets should undergo
     * further analysis, or false to stay in the provisioned state.
     */
    private Boolean analyze = true;

    private List<AssetSpec> files;

    public AssetUploadBuilder() {
        assets = new ArrayList();
        files = new ArrayList();
    }

    /**
     * @param assetSpec
     * @return
     */
    public AssetUploadBuilder addAsset(AssetSpec assetSpec) {
        this.assets.add(assetSpec);
        return this;
    }

    /**
     * Add an AssetSpec to Assets List
     *
     * @param assetSpecUrl
     * @return
     */
    public AssetUploadBuilder addAsset(String assetSpecUrl) {
        this.assets.add(new AssetSpec(assetSpecUrl));
        return this;
    }

    /**
     * Set Analyze Attribute and returns the same instance.
     *
     * @param analyze
     * @return
     */
    public AssetUploadBuilder withAnalyze(Boolean analyze) {
        this.analyze = analyze;
        return this;
    }

    /**
     * Set Assets attribute and returns the same instance.
     *
     * @param assetsSpecList
     * @return
     */
    public AssetUploadBuilder withAssets(List<AssetSpec> assetsSpecList) {
        this.assets = assetsSpecList;
        return this;
    }


}
