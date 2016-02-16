package com.zorroa.analyst.repository;

import com.zorroa.archivist.sdk.domain.AnalyzeResult;
import com.zorroa.archivist.sdk.domain.AssetBuilder;

import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
public interface AssetDao {

    AnalyzeResult bulkUpsert(List<AssetBuilder> builders);


}
