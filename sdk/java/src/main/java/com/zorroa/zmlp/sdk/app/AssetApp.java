package com.zorroa.zmlp.sdk.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.Asset.*;
import jdk.internal.util.xml.impl.Input;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     *
     * @param assetSpecList List of files to upload
     * @return Response State after provisioning assets.
     */
    public BatchCreateAssetResponse uploadFiles(List<AssetSpec> assetSpecList) {

        List<String> uris = assetSpecList.stream().map(assetSpec -> assetSpec.getUri()).collect(Collectors.toList());
        Map body = new HashMap();
        body.put("assets", assetSpecList);

        return client.uploadFiles("/api/v3/assets/_batchUpload", uris, body, BatchCreateAssetResponse.class);

    }

    public JsonNode search(AssetSearch assetSearch){
        return client.post("/api/v3/assets/_search", assetSearch, JsonNode.class);
    }
}
