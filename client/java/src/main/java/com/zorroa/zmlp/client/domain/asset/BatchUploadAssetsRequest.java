package com.zorroa.zmlp.client.domain.asset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Defines the properties required to batch upload a list of assets.
 */
public class BatchUploadAssetsRequest {

    /**
     * A list of AssetSpec objects which define the Assets starting metadata.
     */
    private List<AssetSpec> assets;

    /**
     * Set to true if the assets should undergo further analysis, or false to stay in the provisioned state.
     */
    private Boolean analyze;

    /**
     * The pipeline modules to execute if any, otherwise utilize the default Pipeline.
     */
    private List<String> modules;

    /**
     * A list of available credentials for the analysis job.
     */
    private Set<String> credentials;

    public BatchUploadAssetsRequest() {
        this.assets = new ArrayList();
        this.modules = new ArrayList();
        this.credentials = new HashSet();
    }

    public List<AssetSpec> getAssets() {
        return assets;
    }

    public BatchUploadAssetsRequest addAsset(String assetSpec) {
        this.assets.add(new AssetSpec(assetSpec));
        return this;
    }

    public BatchUploadAssetsRequest addAsset(AssetSpec assetSpec) {
        this.assets.add(assetSpec);
        return this;
    }

    public BatchUploadAssetsRequest setAssets(List<AssetSpec> assets) {
        this.assets = assets;
        return this;
    }

    public Boolean getAnalyze() {
        return analyze;
    }

    public BatchUploadAssetsRequest setAnalyze(Boolean analyze) {
        this.analyze = analyze;
        return this;
    }

    public List<String> getModules() {
        return modules;
    }

    public BatchUploadAssetsRequest setModules(List<String> modules) {
        this.modules = modules;
        return this;
    }

    public Set<String> getCredentials() {
        return credentials;
    }

    public BatchUploadAssetsRequest setCredentials(Set<String> credentials) {
        this.credentials = credentials;
        return this;
    }
}
