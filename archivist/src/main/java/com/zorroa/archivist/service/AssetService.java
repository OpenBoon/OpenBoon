package com.zorroa.archivist.service;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.AssetIndexResult;
import com.zorroa.sdk.domain.Link;
import com.zorroa.sdk.processor.Source;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface AssetService {

    Asset get(String id);

    Asset get(Path path);

    /**
     * Fetch the first page of assets.
     *
     * @return
     */
    PagedList<Asset> getAll(Paging page);

    Asset index(Source source, Link link);
    Asset index(Source source);
    /**
     * Index the given list of sources, optionally attaching the given
     * source link to created assets.
     *
     * @param sources
     * @param link
     * @return
     */
    AssetIndexResult index(List<Source> sources, Link link);
    AssetIndexResult index(List<Source> sources);

    Map<String, Boolean> removeLink(String type, String value, List<String> assets);
    Map<String, Boolean>  appendLink(String type, String value, List<String> assets);

    boolean exists(Path path);

    /**
     * Update the given assetId with the supplied Map of attributes.  Return
     * the new version number of the asset.
     *
     * @param id
     * @param attrs
     * @return
     */
    long update(String id, Map<String, Object> attrs);
}
