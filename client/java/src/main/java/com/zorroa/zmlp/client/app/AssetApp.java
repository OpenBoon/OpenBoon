package com.zorroa.zmlp.client.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zorroa.zmlp.client.Json;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.ZmlpClientException;
import com.zorroa.zmlp.client.domain.asset.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
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
    public BatchCreateAssetsResponse importFiles(AssetCreateBuilder assetCreateBuilder) {
        return client.post("/api/v3/assets/_batch_create", assetCreateBuilder, BatchCreateAssetsResponse.class);
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
    public BatchCreateAssetsResponse uploadFiles(List<AssetSpec> assetSpecList) {

        BatchUploadAssetsRequest batchUploadAssetsRequest = new BatchUploadAssetsRequest().setAssets(assetSpecList);

        BatchCreateAssetsResponse batchCreateAssetsResponse = client.uploadFiles("/api/v3/assets/_batch_upload", batchUploadAssetsRequest, BatchCreateAssetsResponse.class);

        return batchCreateAssetsResponse;
    }

    /**
     * @param assetSpecList Array of files to upload
     * @return Response State after provisioning assets.
     */
    public BatchCreateAssetsResponse uploadFiles(AssetSpec ...assetSpecList){
        return uploadFiles(Arrays.asList(assetSpecList));
    }

    /**
     * @param batchAssetSpec Batch of Asset Specification
     * @return Response State after provisioning assets.
     */
    public BatchCreateAssetsResponse uploadFiles(BatchUploadAssetsRequest batchAssetSpec){
        return client.uploadFiles("/api/v3/assets/_batch_upload", batchAssetSpec, BatchCreateAssetsResponse.class);
    }

    /**
     * Perform an asset search using the ElasticSearch query DSL.  Note that for
     * load and security purposes, not all ElasticSearch search options are accepted.
     * <p>
     * See Also:
     * For search/query format.
     * https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html
     *
     * @param assetSearch The Elastic Search and Element Query in Map format
     * @return A AssetSearchResult containing assets ElasticSearch search.
     */

    public AssetSearchResult search(Map assetSearch) {
        return new AssetSearchResult(this.client, assetSearch);
    }

    /**
     *  Perform an asset search using the ElasticSearch query DSL.  Note that for
     *  load and security purposes, not all ElasticSearch search options are accepted.
     *  <p>
     *  See Also:
     *  For search/query format.
     *  https://www.elastic.co/guide/en/elasticsearch/reference/6.4/search-request-body.html
     *
     * @param searchSourceBuilder
     * @return
     */
    public AssetSearchResult search(SearchSourceBuilder searchSourceBuilder){
        Map assetSearch;
        try {
            assetSearch = Json.mapper.readValue(searchSourceBuilder.toString(), Map.class);
        } catch (JsonProcessingException e) {
            throw new ZmlpClientException("Bad Json Format", e);
        }
        return new AssetSearchResult(this.client, assetSearch);
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
        return new AssetSearchResult(this.client, assetSearch).rawResponse();
    }

    /**
     * Perform an asset scrolled search using the ElasticSearch query DSL.
     *
     * @param search The ElasticSearch search, in Map format, to execute
     * @param timeout The scroll timeout.
     * @return An AssetSearchScroller instance
     */
    public AssetSearchScroller scrollSearch(Map search, String timeout){
        search = Optional.ofNullable(search).orElse(new HashMap());
        timeout = Optional.ofNullable(timeout).orElse("1m");
        return new AssetSearchScroller(this.client, search, timeout);

    }

    /**
     * Perform an asset scrolled search using the ElasticSearch query DSL.
     *
     * @param search The ElasticSearch search to execute
     * @param timeout The scroll timeout.
     * @return An AssetSearchScroller instance
     */
    public AssetSearchScroller scrollSearch(SearchSourceBuilder search, String timeout){
        Map assetSearch;
        try {
            assetSearch = Json.mapper.readValue(search.toString(), Map.class);
        } catch (JsonProcessingException e) {
            throw new ZmlpClientException("Bad Json Format", e);
        }
        return this.scrollSearch(assetSearch, timeout);
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
}
