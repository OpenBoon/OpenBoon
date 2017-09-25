package com.zorroa.archivist.repository;

import com.zorroa.sdk.domain.*;
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

    Document get(String id, String type, String parent);

    Map<String, Object> getProtectedFields(String id);

    boolean exists(Path path);

    boolean exists(String id);

    Asset get(Path path);

    Map<String, List<Object>> removeLink(String type, Object value, List<String> assets);

    Map<String, List<Object>> appendLink(String type, Object value, List<String> assets);

    long update(String assetId, Map<String, Object> attrs);

    Asset index(Document source);

    /**
     * Index the given sources.  If any assets are created, attach a source link.
     * @param sources
     * @return
     */
    AssetIndexResult index(List<Document> sources);

    PagedList<Document> getElements(String assetId, Pager page);

    Map<String, Object> getMapping();
}
