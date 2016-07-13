package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.processor.Source;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface AssetService {

    Asset index(Source source);

    Asset get(String id);

    Asset get(Path path);

    /**
     * Fetch the first page of assets.
     *
     * @return
     */
    List<Asset> getAll();

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
