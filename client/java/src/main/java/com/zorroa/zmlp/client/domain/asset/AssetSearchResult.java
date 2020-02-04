package com.zorroa.zmlp.client.domain.asset;

import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.Page;
import com.zorroa.zmlp.client.domain.PagedList;

import java.util.*;

/**
 * Stores a search result from ElasticSearch and provides some convenience methods
 * for accessing the data.
 */
public class AssetSearchResult {

    /**
     * Search query
     */
    private Map<String, Object> search;

    /**
     * Search Result
     */
    private Map<String, Object> result;


    private ZmlpClient client;

    /**
     * An ElasticSearch query search.
     * @param client
     * @param search
     */
    public AssetSearchResult(ZmlpClient client, Map<String, Object> search) {
        this.client = client;
        this.search = search;
        this.result = client.post("/api/v3/assets/_search", search, Map.class);
    }

    /**
     * A list of assets returned by the query. This is not all of the matches,
     * just a single page of results.
     *
     * @return Paged List of Assets
     */
    public PagedList<Asset> assets() {
        Map hits = (Map) this.result.get("hits");
        List<Asset> assetList = new ArrayList();
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

    /**
     * The number assets in this page.  See "total_size" for the total number of assets matched.
     *
     * @return The number of assets in this page.
     */
    public Integer size() {
        return ((List) ((Map) this.result.get("hits")).get("hits")).size();
    }

    /**
     * The total number of assets matched by the query.
     * @return The total number of assets matched.
     */

    public Integer totalSize() {
        return (Integer) ((Map) ((Map) this.result.get("hits")).get("total")).get("value");
    }

    /**
     * The raw ES response.
     * @return The raw SearchResponse returned by ElasticSearch
     */
    public Map rawResponse(){
        return this.result;
    }

    /**
     * Return an AssetSearchResult containing the next page.
     * @return The next page
     */
    public AssetSearchResult nextPage(){
        Map<String, Object> newSearch = new HashMap();
        newSearch.putAll(search);

        Integer newFrom  = (((Integer)newSearch.getOrDefault("from", 0)) + ((Integer)newSearch.getOrDefault("size", 32)));
        newSearch.put("from", newFrom);

        return new AssetSearchResult(this.client, newSearch);
    }
}
