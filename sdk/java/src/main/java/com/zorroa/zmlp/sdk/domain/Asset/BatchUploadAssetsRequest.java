package com.zorroa.zmlp.sdk.domain.Asset;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the properties required to batch upload a list of assets.
 */
public class BatchUploadAssetsRequest {

    /**
     * A list of AssetSpec objects which define the Assets starting metadata.
     */
    private List<AssetSpec> assets;

    /**
     * Set to true if the assets should undergo
     * further analysis, or false to stay in the provisioned state.
     */
    private Boolean analyze = true;

    /**
     * The analysis to apply.
     */
    private List<String> analysis;

    private List files = new ArrayList<AssetSpec>();


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

    public List getFiles() {
        return files;
    }

    public void setFiles(List files) {
        this.files = files;
    }
}
