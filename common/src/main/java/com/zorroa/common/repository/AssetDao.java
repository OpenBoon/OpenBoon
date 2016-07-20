package com.zorroa.common.repository;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.AssetIndexResult;
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

    long update(String assetId, Map<String, Object> attrs);

    Asset index(Source source);

    AssetIndexResult index(List<Source> sources);

    AssetIndexResult index(String type, List<Source> sources);

    int addToFolder(int folder, List<String> assetIds);

    int removeFromFolder(int folder, List<String> assetIds);
}
