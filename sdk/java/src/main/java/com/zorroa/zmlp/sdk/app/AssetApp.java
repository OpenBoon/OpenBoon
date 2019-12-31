package com.zorroa.zmlp.sdk.app;

import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.Asset.Asset;
import com.zorroa.zmlp.sdk.domain.Asset.BatchCreateAssetRequest;
import com.zorroa.zmlp.sdk.domain.Asset.BatchCreateAssetResponse;

public class AssetApp {

    public final ZmlpClient client;

    public AssetApp(ZmlpClient client) {
        this.client = client;
    }

    /**
     * Import a list of FileImport instances.
     *
     * @param batchCreateAssetRequest The list of files to import as Assets.
     * @return A dictionary containing the provisioning status of each asset, a list of assets to be processed, and a analysis job id.
     */
    public BatchCreateAssetResponse importFiles(BatchCreateAssetRequest batchCreateAssetRequest) {
        return client.post("/api/v3/assets/_batchCreate", batchCreateAssetRequest, BatchCreateAssetResponse.class);
    }

    /**
     * @param id The unique ID of the asset.
     * @return The Asset
     */

    public Asset getById(String id) {
        return client.get(String.format("/api/v3/assets/%s", id), null,Asset.class);
    }
}
