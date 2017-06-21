package com.zorroa.archivist.repository;

import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Source;
import org.elasticsearch.action.search.SearchRequestBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;


public interface AssetDao {

    void removeFields(String assetId, Set<String> fields, boolean refresh);

    boolean delete(String id);

    Asset get(String id);

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

    void getAll(Pager page, SearchRequestBuilder search, OutputStream stream, Map<String, Object> attrs) throws IOException;

    /**
     * Get all assets by page.
     *
     * @param page
     * @return
     */
    PagedList<Asset> getAll(Pager page);

    boolean exists(Path path);

    boolean exists(String id);

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

    Map<String, Object> getMapping();
}
