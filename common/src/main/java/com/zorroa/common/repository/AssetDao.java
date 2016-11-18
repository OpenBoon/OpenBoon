package com.zorroa.common.repository;

import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Source;
import org.elasticsearch.action.search.SearchRequestBuilder;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;


public interface AssetDao {

    Asset get(String id);

    Map<String, List<Object>> removePermission(String type, int permId, List<String> assets);

    Map<String, List<Object>> appendPermission(String type, int permId, List<String> assets);

    /**
     * Return the next page of an asset scroll.
     *
     * @param scrollId
     * @param scrollTimeout
     * @return
     */
    PagedList<Asset> getAll(String scrollId, String scrollTimeout);

    /**
     * Get all assets given the page and SearchRequestBuilder.
     *
     * @param page
     * @param search
     * @return
     */
    PagedList<Asset> getAll(Pager page, SearchRequestBuilder search);

    /**
     * Get all assets by page.
     *
     * @param page
     * @return
     */
    PagedList<Asset> getAll(Pager page);

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
    DocumentIndexResult index(List<Source> sources, LinkSpec sourceLink);

}
