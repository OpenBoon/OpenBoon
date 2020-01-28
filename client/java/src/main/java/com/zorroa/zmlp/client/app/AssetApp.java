package com.zorroa.zmlp.client.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zorroa.zmlp.client.Json;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.Page;
import com.zorroa.zmlp.client.domain.PagedList;
import com.zorroa.zmlp.client.domain.ZmlpClientException;
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

    /**
     * Update a given Asset with a partial document dictionary.
     *
     * @param assetId  Unique Asset Id
     * @param document The changes to apply.
     * @return
     */

    public Map update(String assetId, Map<String, Object> document) {
        String id = Optional.of(assetId).orElseThrow(() -> new ZmlpClientException("Asset Id is missing"));
        Map body = new HashMap();
        body.put("doc", document);

        return client.post(String.format("/api/v3/assets/%s/_update", id), body, Map.class);
    }

    /**
     * Reindex multiple existing assets.  The metadata for the entire asset
     * is overwritten by the local copy.
     *
     * @param assetList
     * @return An ES BulkResponse object.
     */

    public Map batchIndex(List<Asset> assetList) {
        Map<String, Object> body =
                assetList
                        .stream()
                        .collect(Collectors.toMap(Asset::getId, Asset::getDocument));

        return client.post("/api/v3/assets/_batch_index", body, Map.class);
    }

    /**
     *
     * @param assetList List of assets to be updated
     * @return An ES BulkResponse object.
     */
    public Map batchUpdate(List<Asset> assetList) {
        Map<String, Object> body = new HashMap();

        assetList.forEach(asset -> {
            Map<String, Object> map = new HashMap();
            map.put("doc", asset.getDocument());
            body.put(asset.getId(), map);
        });


        return client.post("/api/v3/assets/_batch_update", body, Map.class);
    }

    /**
     * Delete the given asset.
     *
     * @param asset Asset instance.
     * @return An ES Delete response.
     */

    public Map delete(Asset asset){
        String id = Optional.of(asset.getId()).orElseThrow(() -> new ZmlpClientException("Asset Id is missing"));
        return client.delete(String.format("/api/v3/assets/%s", id), null, Map.class);

    }

    /**
     * Delete assets by the given search in Map format.
     * @param query An ES search.
     * @return An ES delete by query response.
     */
    public Map deleteByQuery(Map query){
        return client.delete("/api/v3/assets/_delete_by_query", query, Map.class);
    }

    /**
     * Delete assets by the given search in String format.
     *
     * @param queryString An ES search.
     * @return An ES delete by query response.
     * @throws JsonProcessingException
     */
    public Map deleteByQuery(String queryString) throws JsonProcessingException {
        Map queryMap = Json.mapper.readValue(queryString, Map.class);
        return deleteByQuery(queryMap);
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
