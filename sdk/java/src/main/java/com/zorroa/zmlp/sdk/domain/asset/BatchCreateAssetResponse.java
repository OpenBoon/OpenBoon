package com.zorroa.zmlp.sdk.domain.asset;

import java.util.List;
import java.util.UUID;

/**
 * The response returned after provisioning assets.
 */
public class BatchCreateAssetResponse {

    /**
     * The initial state of the assets added to the database.
     */
    private List<Asset> assets;

    /**
     * A map of the assetId to provisioned status.
     * An asset will fail to provision if it already exists.
     */
    private List<BatchAssetOpStatus> status;

    /**
     * The ID of the analysis job, if analysis was selected
     */
    private UUID jobId;



    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public List<BatchAssetOpStatus> getStatus() {
        return status;
    }

    public void setStatus(List<BatchAssetOpStatus> status) {
        this.status = status;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
}
