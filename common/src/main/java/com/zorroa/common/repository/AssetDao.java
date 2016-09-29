package com.zorroa.common.repository;

import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.ElasticPagedList;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.AssetIndexResult;
import com.zorroa.sdk.domain.LinkSpec;
import com.zorroa.sdk.processor.Source;
import org.elasticsearch.action.search.SearchRequestBuilder;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;


public interface AssetDao {

    Asset get(String id);

    /**
     * Return the next page of an asset scroll.
     *
     * @param scrollId
     * @param scrollTimeout
     * @return
     */
    ElasticPagedList<Asset> getAll(String scrollId, String scrollTimeout);

    /**
     * Get all assets given the page and SearchRequestBuilder.
     *
     * @param page
     * @param search
     * @return
     */
    ElasticPagedList<Asset> getAll(Paging page, SearchRequestBuilder search);

    /**
     * Get all assets by page.
     *
     * @param page
     * @return
     */
    ElasticPagedList<Asset> getAll(Paging page);

    boolean exists(Path path);

    Asset get(Path path);

    Map<String, List<Object>> removeLink(String type, Object value, List<String> assets);

    Map<String, List<Object>> appendLink(String type, Object value, List<String> assets);

    long update(String assetId, Map<String, Object> attrs);

    /**
     * Index the given source.  If an asset is created, attach a source link.
     * @param source
     * @param sourceLink
     * @return
     */
    Asset index(Source source, LinkSpec sourceLink);

    /**
     * Index the given sources.  If any assets are created, attach a source link.
     * @param sources
     * @param sourceLink
     * @return
     */
    AssetIndexResult index(List<Source> sources, LinkSpec sourceLink);

}
