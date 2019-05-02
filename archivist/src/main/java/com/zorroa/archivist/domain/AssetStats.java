package com.zorroa.archivist.domain;

public class AssetStats {
    private int assetCreatedCount;
    private int assetReplacedCount;
    private int assetWarningCount;
    private int assetUpdatedCount;
    private int assetErrorCount;
    private int assetTotalCount;

    public int getAssetCreatedCount() {
        return assetCreatedCount;
    }

    public AssetStats setAssetCreatedCount(int assetCreatedCount) {
        this.assetCreatedCount = assetCreatedCount;
        return this;
    }

    public int getAssetReplacedCount() {
        return assetReplacedCount;
    }

    public AssetStats setAssetReplacedCount(int assetReplacedCount) {
        this.assetReplacedCount = assetReplacedCount;
        return this;
    }

    public int getAssetWarningCount() {
        return assetWarningCount;
    }

    public AssetStats setAssetWarningCount(int assetWarningCount) {
        this.assetWarningCount = assetWarningCount;
        return this;
    }

    public int getAssetUpdatedCount() {
        return assetUpdatedCount;
    }

    public AssetStats setAssetUpdatedCount(int assetUpdatedCount) {
        this.assetUpdatedCount = assetUpdatedCount;
        return this;
    }

    public int getAssetTotalCount() {
        return assetTotalCount;
    }

    public AssetStats setAssetTotalCount(int assetTotalCount) {
        this.assetTotalCount = assetTotalCount;
        return this;
    }

    public int getAssetErrorCount() {
        return assetErrorCount;
    }

    public AssetStats setAssetErrorCount(int assetErrorCount) {
        this.assetErrorCount = assetErrorCount;
        return this;
    }
}
