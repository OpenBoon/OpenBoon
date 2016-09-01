package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Asset;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 9/1/16.
 */
public class AssetServiceTests extends AbstractTest {

    @Autowired
    AssetService assetService;

    @Before
    public void init() {
        addTestAssets("set04/standard");
        refreshIndex();
    }

    @Test
    public void testGetAsset() {
        PagedList<Asset> assets = assetService.getAll(Paging.first());
        for (Asset a: assets) {
            assertEquals(a.getId(),
                    assetService.get(Paths.get(a.getAttr("source.path", String.class))).getId());
        }
    }
}
