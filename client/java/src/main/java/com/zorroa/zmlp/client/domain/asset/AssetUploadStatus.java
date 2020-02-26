package com.zorroa.zmlp.client.domain.asset;

import java.util.List;

public class AssetUploadStatus {

    private Integer fileCount;

    private Integer batchNumber;

    /**
     * A map of the assetId to provisioned status.
     * An asset will fail to provision if it already exists.
     */
    private List<BatchCreateAssetsResponse> status;

    public Integer getFileCount() {
        return fileCount;
    }

    public AssetUploadStatus setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
        return this;
    }

    public Integer getBatchNumber() {
        return batchNumber;
    }

    public AssetUploadStatus setBatchNumber(Integer batchNumber) {
        this.batchNumber = batchNumber;
        return this;
    }

    public List<BatchCreateAssetsResponse> getStatus() {
        return status;
    }

    public AssetUploadStatus setStatus(List<BatchCreateAssetsResponse> status) {
        this.status = status;
        return this;
    }
}
