package com.zorroa.common.repository;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.AssetIndexResult;
import com.zorroa.sdk.domain.Link;
import com.zorroa.sdk.processor.Source;
import org.elasticsearch.action.search.SearchRequestBuilder;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;



public interface AssetDao {

    Asset get(String id);

    PagedList<Asset> getAll(Paging page, SearchRequestBuilder search);

    PagedList<Asset> getAll(Paging page);

    boolean exists(Path path);

    Asset get(Path path);

    Map<String, Boolean> removeLink(String type, String value, List<String> assets);

    Map<String, Boolean>  appendLink(String type, String value, List<String> assets);

    long update(String assetId, Map<String, Object> attrs);

    /**
     * Index the given source.  If an asset is created, attach a source link.
     * @param source
     * @param sourceLink
     * @return
     */
    Asset index(Source source, Link sourceLink);

    /**
     * Index the given sources.  If any assets are created, attach a source link.
     * @param sources
     * @param sourceLink
     * @return
     */
    AssetIndexResult index(List<Source> sources, Link sourceLink);

}
