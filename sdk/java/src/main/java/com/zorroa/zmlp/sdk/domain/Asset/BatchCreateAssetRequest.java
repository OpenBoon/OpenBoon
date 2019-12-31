package com.zorroa.zmlp.sdk.domain.Asset;

import com.zorroa.zmlp.sdk.domain.Asset.AssetSpec;

import java.util.List;

public class BatchCreateAssetRequest {

    /**
     * The list of assets to be created
     */
    private List<AssetSpec> assets;

    /**
     * Set to true if the assets should undergo further analysis, or false to stay in the provisioned state.
     */
    private Boolean analyze;

    /**
     * The analysis to apply.
     */
    private List<String> analysis;

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

    public List<String> getAnalysis() {
        return analysis;
    }

    public void setAnalysis(List<String> analysis) {
        this.analysis = analysis;
    }
}
