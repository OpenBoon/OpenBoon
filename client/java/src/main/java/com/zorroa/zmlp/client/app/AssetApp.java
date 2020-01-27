package com.zorroa.zmlp.client.app;

import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.Page;
import com.zorroa.zmlp.client.domain.PagedList;
import com.zorroa.zmlp.client.domain.asset.*;

import java.util.*;
import java.util.stream.Collectors;

public class AssetApp {

    public final ZmlpClient client;

    public AssetApp(ZmlpClient client) {
        this.client = client;
    }

    /**
     * Import a list of FileImport instances.
     *
     * @param assetCreateBuilder The list of files to import as Assets.
     * @return A dictionary containing the provisioning status of each asset, a list of assets to be processed, and a analysis job id.
     */
    public BatchCreateAssetResponse importFiles(AssetCreateBuilder assetCreateBuilder) {
        return client.post("/api/v3/assets/_batchCreate", assetCreateBuilder, BatchCreateAssetResponse.class);
    }

    /**
     * @param id The unique ID of the asset.
     * @return The Asset
     */

    public Asset getById(String id) {
        return client.get(String.format("/api/v3/assets/%s", id), null, Asset.class);
    }

    /**
     * @param assetSpecList List of files to upload
     * @return Response State after provisioning assets.
     */
    public BatchCreateAssetResponse uploadFiles(List<AssetSpec> assetSpecList) {

        List<String> uris = assetSpecList.stream().map(assetSpec -> assetSpec.getUri()).collect(Collectors.toList());
        Map body = new HashMap();
        body.put("assets", assetSpecList);

        return client.uploadFiles("/api/v3/assets/_batchUpload", uris, body, BatchCreateAssetResponse.class);

    }

    /**
     * Perform an asset search using the ElasticSearch query DSL.  Note that for
     * load and security purposes, not all ElasticSearch search options are accepted.
     * <p>
     * See Also:
     * For search/query format.
     * https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html
     *
     * @param assetSearch The Elastic Search and Element Query
     * @return A SearchResult containing assets ElasticSearch search result dictionary.
     */

    public PagedList<Asset> search(Map assetSearch) {
        Map post = client.post("/api/v3/assets/_search", assetSearch, Map.class);

        return buildAssetListResult(post);
    }

    /**
     * Perform an asset search using the ElasticSearch query DSL.  Note that for
     * load and security purposes, not all ElasticSearch search options are accepted.
     * <p>
     * See Also:
     * For search/query format.
     * https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html
     *
     * @param assetSearch The Elastic Search and Element Query
     * @return A SearchResult containing Raw mode an ElasticSearch search result dictionary.
     */

    public Map rawSearch(Map assetSearch) {
        return client.post("/api/v3/assets/_search", assetSearch, Map.class);
    }

    /**
     * Re-index an existing asset.  The metadata for the entire asset
     * is overwritten by the local copy.
     *
     * @param asset The asset
     * @return EL Object
     */

    public Map index(Asset asset) {
        return client.post(String.format("/api/v3/assets/%s/_index", asset.getId()), asset.getDocument(), Map.class);
    }

    private PagedList<Asset> buildAssetListResult(Map map) {

        Map hits = (Map) map.get("hits");
        List<Asset> assetList = new ArrayList<Asset>();
        PagedList<Asset> pagedList = new PagedList(new Page(), assetList);

        if (hits != null) {

            Optional.ofNullable(hits.get("hits")).ifPresent(
                    (hitsHits) -> {
                        ((List<Map>) hitsHits).forEach(hit -> {
                            String id = (String) hit.get("_id");
                            Map document = (Map) hit.get("_source");
                            Asset asset = new Asset(id, document);
                            assetList.add(asset);
                        });
                    }
            );
        }
        return pagedList;
    }


}
