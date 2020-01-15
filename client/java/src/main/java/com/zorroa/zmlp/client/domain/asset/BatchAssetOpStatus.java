package com.zorroa.zmlp.client.domain.asset;

/**
 * Used to describe the result of a batch asset operation
 */
public class BatchAssetOpStatus {

    /**
     * The ID of the asset.
     */
    private String assetId;

    /**
     * A failure message will be set if the operation filed.
     */
    private String failureMessage;
    /**
     * True of the operation failed.
     */
    private Boolean failed;

    public BatchAssetOpStatus() {
        this.failed = false;
    }

    public BatchAssetOpStatus(String assetId, String failureMessage) {
        this.assetId = assetId;
        this.failureMessage = failureMessage;
        this.failed = failureMessage != null;
    }

    public String getAssetId() {
        return assetId;
    }

    public BatchAssetOpStatus setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public BatchAssetOpStatus setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    public Boolean getFailed() {
        return failed;
    }

    public BatchAssetOpStatus setFailed(Boolean failed) {
        this.failed = failed;
        return this;
    }
}
