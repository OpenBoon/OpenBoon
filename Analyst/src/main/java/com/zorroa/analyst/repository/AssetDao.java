package com.zorroa.analyst.repository;

import com.zorroa.analyst.domain.BulkAssetUpsertResult;
import com.zorroa.archivist.sdk.domain.AssetBuilder;

import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
public interface AssetDao {

    BulkAssetUpsertResult bulkUpsert(List<AssetBuilder> builders);


}
