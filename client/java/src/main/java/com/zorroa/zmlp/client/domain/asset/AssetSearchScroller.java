package com.zorroa.zmlp.client.domain.asset;

import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.ZmlpClientException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The AssetSearchScroller can iterate over large amounts of assets without incurring paging
 * overhead by utilizing a server side cursor.  The cursor is held open for the specified
 * timeout time unless it is refreshed before the timeout occurs.  In this sense, it's important
 * to complete whatever operation you're taking on each asset within the timeout time.  For example
 * if your page size is 32 and your timeout is 1m, you have 1 minute to handles 32 assets.  If that
 * is not enough time, consider increasing the timeout or lowering your page size.
 */
public class AssetSearchScroller implements Iterator<List<Asset>> {

    private String scrollId;

    private ZmlpClient client;

    private Map<String, Object> search;

    private Map<String, Object> result;

    private String timeout;

    /**
     * Create a new AssetSearchScroller instance.
     *
     * @param client  A ZmlpClient instance.
     * @param search  The ES search
     * @param timeout Yield the raw ES response rather than assets. The raw response will contain the entire page, not individual assets.
     */
    public AssetSearchScroller(ZmlpClient client, Map<String, Object> search, String timeout) {
        this.client = client;
        this.search = search;
        this.timeout = timeout;

        this.result = client.post(String.format("api/v3/assets/_search?scroll=%s", timeout), search, Map.class);
    }

    @Override
    public boolean hasNext() {
        List<Asset> currentAssetList = getCurrentAssetList();
        return currentAssetList != null && !currentAssetList.isEmpty();
    }

    @Override
    public List<Asset> next() {

        List<Asset> currentResult = getCurrentAssetList();

        if (currentResult == null) return null;

        try {
            // Check if current Scroll Id value is valid
            scrollId = (String) Optional.ofNullable(result.get("_scroll_id")).orElseThrow(() -> new ZmlpClientException("No scroll ID returned with scroll search, has it timed out?"));

            // Load next request
            result = nextRequest();

        } catch (ZmlpClientException ex) {
            //If Scroll Id is not valid, delete and set next request to null.
            deleteRequest(this.scrollId);
            result = null;
        }

        return currentResult;
    }

    private List<Asset> getCurrentAssetList() {
        Map hits = (Map) result.get("hits");

        if (hits == null || hits.get("hits") == null)
            return null;

        List hitsHits = (List) hits.get("hits");

        //Current Value
        List<Asset> currentResult = (List<Asset>) hitsHits.stream().map(h ->
        {
            Map asset = (Map) h;
            return new Asset((String) asset.get("_id"), (Map<String, Object>) asset.get("_source"));
        }).collect(Collectors.toList());
        return currentResult;
    }

    private Map<String, Object> nextRequest() {
        Map body = new HashMap();
        body.put("scroll", this.timeout);
        body.put("scroll_id", this.scrollId);
        return client.post("api/v3/assets/_search/scroll", body, Map.class);
    }

    private Map<String, Object> deleteRequest(String scrollId) {
        Map body = new HashMap();
        body.put("scroll_id", scrollId);
        return client.delete("api/v3/assets/_search/scroll", body, Map.class);

    }
}
