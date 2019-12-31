package com.zorroa.zmlp.sdk.domain.Asset;

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

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public Boolean getFailed() {
        return failed;
    }

    public void setFailed(Boolean failed) {
        this.failed = failed;
    }
}
