package com.zorroa.zmlp.client.domain.asset;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The response returned after provisioning assets.
 */
public class BatchCreateAssetsResponse {

    /**
     * A map of failed asset ids to error message
     */
    private List<Map<String, String>> failed;

    /**
     * A list of asset Ids created.
     */
    private List<String> created;
    /**
     * "The assets that already existed."
     */
    private List<String> exists;

    /**
     * The ID of the analysis job, if analysis was selected
     */
    private UUID jobId;

    /**
     * The total number of assets to be updated.
     */
    public Integer totalUpdated() {
        return created.size() + exists.size();
    }

    public List<Map<String, String>> getFailed() {
        return failed;
    }

    public BatchCreateAssetsResponse setFailed(List<Map<String, String>> failed) {
        this.failed = failed;
        return this;
    }

    public List<String> getCreated() {
        return created;
    }

    public BatchCreateAssetsResponse setCreated(List<String> created) {
        this.created = created;
        return this;
    }

    public List<String> getExists() {
        return exists;
    }

    public BatchCreateAssetsResponse setExists(List<String> exists) {
        this.exists = exists;
        return this;
    }

    public UUID getJobId() {
        return jobId;
    }

    public BatchCreateAssetsResponse setJobId(UUID jobId) {
        this.jobId = jobId;
        return this;
    }
}