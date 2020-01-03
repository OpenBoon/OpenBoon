package com.zorroa.zmlp.sdk.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.Asset.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * @param assetSearch Asset search object that contains The Elastic Search and Element Query
     * @param raw         Return the raw ElasticSearch dict result rather than a SearchResult
     * @return A SearchResult containing assets or in raw mode an ElasticSearch search result dictionary.
     */

    public JsonNode search(AssetSearch assetSearch, Boolean raw) {
        JsonNode post = client.post("/api/v3/assets/_search", assetSearch, JsonNode.class);
        if (raw)
            return post;
        else {
            Integer from = (Integer) assetSearch.getSearch().get("from");
            Integer fromValue = Optional.ofNullable(from).orElse(0);
            ((ObjectNode) post.get("hits")).put("offset", fromValue);
            return buildNonRawSearchResult(post);
        }
    }

    private JsonNode buildNonRawSearchResult(JsonNode jsonNode) {
        JsonNode newNode = JsonNodeFactory.instance.objectNode();
        ObjectNode objectNode = (ObjectNode) newNode;

        JsonNode hits = jsonNode.get("hits");
        if (hits != null) {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();

            Optional.ofNullable(hits.get("hits")).ifPresent(
                    hitsHits -> {
                        hitsHits.forEach(hit -> {
                            JsonNode h = JsonNodeFactory.instance.objectNode();
                            JsonNode id = hit.get("_id");
                            ((ObjectNode) h).put("id", id);
                            ((ObjectNode) h).put("document", hit.get("_source"));
                            arrayNode.add(h);
                        });

                    }
            );

            objectNode.put("items", arrayNode);
            objectNode.put("offset", hits.get("offset"));

            objectNode.put("size", arrayNode.size());

            JsonNode total = hits.get("total");
            objectNode.put("total", total != null ? total.get("value") : null);

        } else {

            Optional.ofNullable(hits.get("list")).ifPresent(
                    hit -> objectNode.put("list", hit)
            );

            JsonNode page = objectNode.get("page");
            if (page != null) {
                objectNode.put("offset", page.get("from"));
                objectNode.put("total", page.get("totalCount"));
            }

            Optional.ofNullable(objectNode.get("list"))
                    .ifPresent(value -> objectNode.put("size", value.size()));
        }

        return newNode;
    }

    public JsonNode search(AssetSearch assetSearch) {
        return this.search(assetSearch, false);
    }
}
